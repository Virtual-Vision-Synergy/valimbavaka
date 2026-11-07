import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { demo } from '../../../shared/data-access/demo';
import { PageResponse } from '../../../shared/models/page-response';
import { DRIVER_ROUND_FIXTURE } from './delivery-fixtures';
import { businessToday, toBoard } from './delivery-mapping';
import {
  DeliveryBoardRDto,
  DeliveryDriverRDto,
  DeliveryOrderRDto,
  DeliveryOrderWDto,
  DeliveryStatusWDto,
  DriverRoundRDto,
} from './delivery-models';

/** Taille de page maximale du serveur : une journée tient sur une page. */
const MAX_PAGE_SIZE = 100;

/**
 * Livraisons. Le tableau du jour lit `GET /api/v1/deliveries?date=` et la liste des livreurs ; la création
 * (LIV-01) passe par la file hors ligne (`OutboxService`, SYS-07). La tournée du livreur (LIV-02) et le
 * changement de statut (LIV-03, LIV-04) restent des données de démonstration.
 */
@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/deliveries';

  /** Commandes du jour (Antananarivo) dans l'ordre des arrêts, regroupées par tournée de livreur. */
  board(day: string = businessToday()): Observable<DeliveryBoardRDto> {
    const params = new HttpParams()
      .set('date', day)
      .set('page', 1)
      .set('size', MAX_PAGE_SIZE)
      .set('sortBy', 'stopNumber')
      .set('sortDirection', 'ASC');
    return forkJoin({
      orders: this.http.get<PageResponse<DeliveryOrderRDto>>(this.baseUrl, { params }),
      drivers: this.drivers(),
    }).pipe(map(({ orders, drivers }) => toBoard(orders.items, drivers)));
  }

  /** Livreurs actifs proposés à l'affectation, par prénom (`deliveries.create`). */
  drivers(): Observable<DeliveryDriverRDto[]> {
    const params = new HttpParams().set('page', 1).set('size', MAX_PAGE_SIZE);
    return this.http
      .get<PageResponse<DeliveryDriverRDto>>(`${this.baseUrl}/drivers`, { params })
      .pipe(map((page) => page.items));
  }

  /** Crée la commande (statut `TO_PREPARE`, ajoutée en fin de tournée) ; `queued` si elle attend le réseau. */
  create(body: DeliveryOrderWDto): Observable<SubmitResult<DeliveryOrderRDto>> {
    return this.outbox.submit<DeliveryOrderRDto>('POST', this.baseUrl, body);
  }

  myRound(): Observable<DriverRoundRDto> {
    return demo(DRIVER_ROUND_FIXTURE);
  }

  /** Simulation : accepte le changement de statut ; aucune donnée n'est conservée. */
  updateStatus(deliveryId: string, change: DeliveryStatusWDto): Observable<string> {
    return demo(change).pipe(map(() => deliveryId));
  }
}
