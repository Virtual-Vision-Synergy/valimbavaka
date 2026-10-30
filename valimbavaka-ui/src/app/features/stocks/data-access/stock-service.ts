import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { ReferentialService } from '../../referential/data-access/referential-service';
import { LocationTotals, toLocationTotals, toStockRow } from './stock-mapping';
import { StockRDto, StockRowRDto, StockValuationRDto } from './stock-models';

/** Une page de 100 articles actifs, triés par nom (taille maximale du serveur). */
const ALL_ARTICLES: PageQuery = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' };

/**
 * Stocks par emplacement : lecture seule de `GET /api/v1/stocks`. Le stock est la somme des mouvements
 * typés côté serveur et ne s'écrit jamais depuis l'interface (STK-01).
 */
@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly referential = inject(ReferentialService);
  private readonly baseUrl = '/api/v1/stocks';

  list(): Observable<readonly StockRowRDto[]> {
    return forkJoin({
      stocks: this.http.get<PageResponse<StockRDto>>(this.baseUrl, {
        params: toHttpParams(ALL_ARTICLES),
      }),
      ranges: this.referential.listProductRanges(),
    }).pipe(map(({ stocks, ranges }) => stocks.items.map((stock) => toStockRow(stock, ranges))));
  }

  /** Quantité et valeur au coût de tout le stock, par emplacement (STK-03) ; réservé à `stocks.value`. */
  valuation(): Observable<LocationTotals> {
    return this.http
      .get<StockValuationRDto>(`${this.baseUrl}/valuation`)
      .pipe(map(toLocationTotals));
  }
}
