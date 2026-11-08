import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { AuthStore } from '../../../core/auth/auth-store';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { demo } from '../../../shared/data-access/demo';
import { PageResponse } from '../../../shared/models/page-response';
import { businessToday, toBoard, toDriverRound } from './delivery-mapping';
import {
  DeliveryBoardRDto,
  DeliveryDriverRDto,
  DeliveryEventRDto,
  DeliveryOrderRDto,
  DeliveryOrderWDto,
  DeliveryStatusWDto,
  DriverRoundRDto,
} from './delivery-models';

/** Taille de page maximale du serveur : une journée tient sur une page. */
const MAX_PAGE_SIZE = 100;

/** Une journée sur une page, dans l'ordre des arrêts. */
function dayParams(day: string): HttpParams {
  return new HttpParams()
    .set('date', day)
    .set('page', 1)
    .set('size', MAX_PAGE_SIZE)
    .set('sortBy', 'stopNumber')
    .set('sortDirection', 'ASC');
}

/**
 * Livraisons. Le tableau d'un jour lit `GET /api/v1/deliveries?date=` et la liste des livreurs, l'historique d'une
 * commande `GET /api/v1/deliveries/{id}/history` (LIV-06) ; la création
 * (LIV-01) passe par la file hors ligne (`OutboxService`, SYS-07) ; la tournée du livreur (LIV-02) lit ses
 * commandes du jour. Le changement de statut (LIV-03, LIV-04) reste une simulation.
 */
@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly authStore = inject(AuthStore);
  private readonly baseUrl = '/api/v1/deliveries';

  /**
   * Commandes d'un jour (Antananarivo, aujourd'hui par défaut) dans l'ordre des arrêts, regroupées par tournée de
   * livreur ; un jour passé donne l'historique de ce jour (LIV-06).
   */
  board(day: string = businessToday()): Observable<DeliveryBoardRDto> {
    return forkJoin({
      orders: this.http.get<PageResponse<DeliveryOrderRDto>>(this.baseUrl, {
        params: dayParams(day),
      }),
      drivers: this.drivers(),
    }).pipe(map(({ orders, drivers }) => toBoard(orders.items, drivers)));
  }

  /**
   * Tournée du jour du livreur connecté (LIV-02) : ses commandes, arrêt par arrêt. Le serveur limite déjà un
   * livreur à ses commandes ; `driverId` évite qu'un administrateur voie toutes les tournées mélangées.
   */
  myRound(day: string = businessToday()): Observable<DriverRoundRDto> {
    const user = this.authStore.currentUser();
    const driver = {
      id: user?.id ?? '',
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
    };
    const params = dayParams(day).set('driverId', driver.id);
    return this.http
      .get<PageResponse<DeliveryOrderRDto>>(this.baseUrl, { params })
      .pipe(map((page) => toDriverRound(page.items, driver, day)));
  }

  /** Historique d'une commande (LIV-06) : ses lignes du journal, de la plus ancienne à la plus récente. */
  history(orderId: string): Observable<DeliveryEventRDto[]> {
    const params = new HttpParams().set('page', 1).set('size', MAX_PAGE_SIZE);
    return this.http
      .get<PageResponse<DeliveryEventRDto>>(`${this.baseUrl}/${orderId}/history`, { params })
      .pipe(map((page) => page.items));
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

  /** Simulation : accepte le changement de statut ; aucune donnée n'est conservée. */
  updateStatus(deliveryId: string, change: DeliveryStatusWDto): Observable<string> {
    return demo(change).pipe(map(() => deliveryId));
  }
}
