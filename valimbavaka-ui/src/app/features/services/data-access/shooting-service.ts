import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { StockRDto } from '../../stocks/data-access/stock-models';
import { ShootingRDto, ShootingRowRDto, ShootingSummaryRDto, ShootingWDto } from './service-models';
import { toShootingRow } from './shooting-mapping';

/** Les 100 sorties les plus récentes (taille maximale du serveur). */
const RECENT_SHOOTINGS: PageQuery = {
  page: 1,
  size: 100,
  sortBy: 'createdAt',
  sortDirection: 'DESC',
};

/** Les 100 premiers articles actifs par nom, avec leur stock par emplacement (STK-01). */
const STOCK_BY_NAME: PageQuery = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' };

/**
 * Sorties shooting (SHO-01), réintégration (SHO-02) et indicateurs de suivi (SHO-03) : lecture de
 * `GET /api/v1/shootings`, écritures par la file hors ligne (`OutboxService`, SYS-07) : `queued` vaut vrai si la
 * saisie attend le réseau.
 */
@Injectable({ providedIn: 'root' })
export class ShootingService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/shootings';

  list(): Observable<readonly ShootingRowRDto[]> {
    return this.http
      .get<PageResponse<ShootingRDto>>(this.baseUrl, { params: toHttpParams(RECENT_SHOOTINGS) })
      .pipe(map((page) => page.items.map(toShootingRow)));
  }

  /** Indicateurs des sorties encore dehors (SHO-03) : nombre, retards, valeur au coût (`shootings.track`). */
  summary(): Observable<ShootingSummaryRDto> {
    return this.http.get<ShootingSummaryRDto>(`${this.baseUrl}/summary`);
  }

  /** Articles proposés au formulaire, avec la quantité disponible dans chaque emplacement. */
  stock(): Observable<readonly StockRDto[]> {
    return this.http
      .get<PageResponse<StockRDto>>('/api/v1/stocks', { params: toHttpParams(STOCK_BY_NAME) })
      .pipe(map((page) => page.items));
  }

  create(body: ShootingWDto): Observable<SubmitResult<ShootingRDto>> {
    return this.outbox.submit<ShootingRDto>('POST', this.baseUrl, body);
  }

  /** Réintégration (SHO-02) : les produits reviennent dans l'emplacement de sortie, statut `RETURNED`. */
  returnShooting(id: string): Observable<SubmitResult<ShootingRDto>> {
    return this.outbox.submit<ShootingRDto>('POST', `${this.baseUrl}/${id}/return`);
  }
}
