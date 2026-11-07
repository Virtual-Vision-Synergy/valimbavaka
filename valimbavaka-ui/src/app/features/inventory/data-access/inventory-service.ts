import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { toInventorySheet } from './inventory-sheet';
import {
  InventoryContextRDto,
  InventoryCountWDto,
  InventoryLineRDto,
  InventoryReasonWDto,
  InventorySessionQuery,
  InventorySessionRDto,
  InventorySessionWDto,
} from './inventory-models';

/** Taille de page maximale du serveur : les lignes d'une session sont lues par pages de 100. */
const LINE_PAGE_SIZE = 100;
/** Sessions clôturées affichées à côté de la feuille de comptage (INV-05) ; l'historique complet a sa page. */
const PREVIOUS_SIZE = 5;

/**
 * Sessions d'inventaire branchées sur `/api/v1/inventories` : ouverture (INV-01), feuille de comptage et saisie
 * du comptage (INV-02), motif d'écart par ligne (INV-03), clôture avec ajustement du stock (INV-04), historique des
 * sessions avec leur nombre d'écarts (INV-05, `list`). Les écritures passent par la file hors ligne (SYS-07).
 */
@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/inventories';

  /** Sessions, les plus récentes d'abord ; statut et emplacement envoyés seulement s'ils sont choisis. */
  list(query: InventorySessionQuery): Observable<PageResponse<InventorySessionRDto>> {
    let params = toHttpParams(query);
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.locationId) {
      params = params.set('locationId', query.locationId);
    }
    return this.http.get<PageResponse<InventorySessionRDto>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<InventorySessionRDto> {
    return this.http.get<InventorySessionRDto>(`${this.baseUrl}/${id}`);
  }

  lines(id: string, query: PageQuery): Observable<PageResponse<InventoryLineRDto>> {
    return this.http.get<PageResponse<InventoryLineRDto>>(`${this.baseUrl}/${id}/lines`, {
      params: toHttpParams(query),
    });
  }

  /** Ouvre une session : `queued` si elle attend le réseau (aucun numéro avant la synchronisation). */
  open(body: InventorySessionWDto): Observable<SubmitResult<InventorySessionRDto>> {
    return this.outbox.submit<InventorySessionRDto>('POST', this.baseUrl, body);
  }

  /**
   * Feuille de comptage : la session en cours la plus récente et toutes ses lignes (`session` null s'il n'y en a
   * pas), avec les dernières sessions clôturées (INV-05), lues en parallèle des lignes.
   */
  current(): Observable<InventoryContextRDto> {
    return this.list({ page: 1, size: 1, status: 'IN_PROGRESS' }).pipe(
      switchMap((page) => {
        const session = page.items[0];
        if (!session) {
          return of<InventoryContextRDto>({ session: null, previous: [] });
        }
        return forkJoin({
          lines: this.allLines(session.id),
          previous: this.list({ page: 1, size: PREVIOUS_SIZE, status: 'CLOSED' }),
        }).pipe(
          map(({ lines, previous }) => ({
            session: toInventorySheet(session, lines),
            previous: previous.items,
          })),
        );
      }),
    );
  }

  /** Enregistre la quantité comptée d'une ligne : `queued` si elle attend le réseau. */
  count(
    sessionId: string,
    lineId: string,
    body: InventoryCountWDto,
  ): Observable<SubmitResult<InventoryLineRDto>> {
    const url = `${this.baseUrl}/${sessionId}/lines/${lineId}/count`;
    return this.outbox.submit<InventoryLineRDto>('PUT', url, body);
  }

  /** Enregistre le motif de l'écart d'une ligne comptée : `queued` s'il attend le réseau. */
  qualify(
    sessionId: string,
    lineId: string,
    body: InventoryReasonWDto,
  ): Observable<SubmitResult<InventoryLineRDto>> {
    const url = `${this.baseUrl}/${sessionId}/lines/${lineId}/reason`;
    return this.outbox.submit<InventoryLineRDto>('PUT', url, body);
  }

  /**
   * Clôture la session (INV-04) : le serveur ajuste le stock de l'emplacement aux quantités comptées et renvoie la
   * session `CLOSED` ; `queued` si la clôture attend le réseau.
   */
  close(sessionId: string): Observable<SubmitResult<InventorySessionRDto>> {
    return this.outbox.submit<InventorySessionRDto>('POST', `${this.baseUrl}/${sessionId}/close`);
  }

  /** Toutes les lignes d'une session, triées par code article : première page, puis les suivantes en parallèle. */
  private allLines(id: string): Observable<readonly InventoryLineRDto[]> {
    const query = { page: 1, size: LINE_PAGE_SIZE };
    return this.lines(id, query).pipe(
      switchMap((first) => {
        if (first.totalPages <= 1) {
          return of(first.items);
        }
        const next = Array.from({ length: first.totalPages - 1 }, (_, index) =>
          this.lines(id, { ...query, page: index + 2 }),
        );
        return forkJoin(next).pipe(
          map((pages) => [...first.items, ...pages.flatMap((page) => page.items)]),
        );
      }),
    );
  }
}
