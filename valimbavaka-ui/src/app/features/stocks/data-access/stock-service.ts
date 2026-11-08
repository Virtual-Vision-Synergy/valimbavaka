import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { OutboxService } from '../../../core/offline/outbox-service';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { ReferentialService } from '../../referential/data-access/referential-service';
import { LocationTotals, toLocationTotals, toStockRow } from './stock-mapping';
import {
  RestockOrderRDto,
  RestockOrderWDto,
  StockAlertRDto,
  StockRDto,
  StockRowRDto,
  StockValuationRDto,
  SupplierReceiptRDto,
  SupplierReceiptWDto,
} from './stock-models';

/** Une page de 100 articles actifs, triés par nom (taille maximale du serveur). */
const ALL_ARTICLES: PageQuery = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' };

/** Les 100 alertes les plus urgentes : l'ordre est fixé par le serveur. */
const MOST_URGENT_ALERTS: PageQuery = { page: 1, size: 100 };

/** Les 100 bons les plus récents : ceux qui attendent leur livraison sont parmi eux. */
const RECENT_RESTOCK_ORDERS: PageQuery = { page: 1, size: 100 };

/**
 * Stocks par emplacement : lecture seule de `GET /api/v1/stocks`. Le stock est la somme des mouvements
 * typés côté serveur et ne s'écrit jamais depuis l'interface (STK-01) ; s'écrivent seulement le bon de réappro
 * (STK-05) et la réception fournisseur (STK-06), qui crée elle-même ses mouvements côté serveur.
 */
@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly referential = inject(ReferentialService);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/stocks';

  list(): Observable<readonly StockRowRDto[]> {
    return forkJoin({
      stocks: this.http.get<PageResponse<StockRDto>>(this.baseUrl, {
        params: toHttpParams(ALL_ARTICLES),
      }),
      ranges: this.referential.listProductRanges(),
    }).pipe(map(({ stocks, ranges }) => stocks.items.map((stock) => toStockRow(stock, ranges))));
  }

  /**
   * Stock brut de chaque article actif par emplacement (identifiant du référentiel), une page de 100 triée par
   * nom : sert aux saisies qui sortent du stock d'un emplacement (retour de stock, TRF-03).
   */
  quantities(): Observable<readonly StockRDto[]> {
    return this.http
      .get<PageResponse<StockRDto>>(this.baseUrl, { params: toHttpParams(ALL_ARTICLES) })
      .pipe(map((page) => page.items));
  }

  /** Articles sous leur seuil minimum, ruptures d'abord, avec le manque et la quantité suggérée (STK-04). */
  alerts(): Observable<PageResponse<StockAlertRDto>> {
    return this.http.get<PageResponse<StockAlertRDto>>(`${this.baseUrl}/alerts`, {
      params: toHttpParams(MOST_URGENT_ALERTS),
    });
  }

  /** Quantité et valeur au coût de tout le stock, par emplacement (STK-03) ; réservé à `stocks.value`. */
  valuation(): Observable<LocationTotals> {
    return this.http
      .get<StockValuationRDto>(`${this.baseUrl}/valuation`)
      .pipe(map(toLocationTotals));
  }

  /**
   * Génère un bon de réapprovisionnement à partir des articles sous le seuil (STK-05), restreint aux articles
   * choisis. L'écriture passe par la file hors ligne (`Idempotency-Key`, SYS-07).
   */
  generateRestockOrder(articleIds: readonly string[]): Observable<SubmitResult<RestockOrderRDto>> {
    const body: RestockOrderWDto = { articleIds };
    return this.outbox.submit<RestockOrderRDto>('POST', `${this.baseUrl}/restock-orders`, body);
  }

  /** Bons de réapprovisionnement encore en attente de livraison (STK-05), les plus récents d'abord. */
  pendingRestockOrders(): Observable<readonly RestockOrderRDto[]> {
    return this.http
      .get<PageResponse<RestockOrderRDto>>(`${this.baseUrl}/restock-orders`, {
        params: toHttpParams(RECENT_RESTOCK_ORDERS),
      })
      .pipe(map((page) => page.items.filter((order) => order.status === 'PENDING')));
  }

  /**
   * Enregistre la réception d'une livraison fournisseur (STK-06) : le serveur fait entrer le stock et, si la
   * réception reprend un bon, le passe à `RECEIVED`. L'écriture passe par la file hors ligne (`Idempotency-Key`).
   */
  recordReceipt(body: SupplierReceiptWDto): Observable<SubmitResult<SupplierReceiptRDto>> {
    return this.outbox.submit<SupplierReceiptRDto>('POST', `${this.baseUrl}/receipts`, body);
  }
}
