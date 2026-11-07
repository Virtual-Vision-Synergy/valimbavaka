import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { demo } from '../../../shared/data-access/demo';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { INVENTORY_FIXTURE } from './inventory-fixtures';
import {
  InventoryCloseWDto,
  InventoryContextRDto,
  InventoryLineRDto,
  InventorySessionQuery,
  InventorySessionRDto,
  InventorySessionWDto,
} from './inventory-models';

/**
 * Sessions d'inventaire. Ouverture et lectures branchées sur `/api/v1/inventories` (INV-01) ; l'ouverture passe
 * par la file hors ligne (SYS-07). La feuille de comptage (`current`, `close`) reste en démonstration jusqu'à
 * INV-02 (comptage) et INV-04 (clôture).
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

  /** Démonstration : feuille de comptage (INV-02). */
  current(): Observable<InventoryContextRDto> {
    return demo(INVENTORY_FIXTURE);
  }

  /** Simulation : accepte la clôture et renvoie le numéro de session (INV-04). */
  close(request: InventoryCloseWDto): Observable<InventoryCloseWDto> {
    return demo(request);
  }
}
