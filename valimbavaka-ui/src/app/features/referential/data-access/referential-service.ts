import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { ProductRangeRDto, SalesChannelRDto, StockLocationRDto } from './referential-models';

/** Référentiels courts : une seule page, triée par libellé. */
const ALL_ITEMS = { page: 1, size: 100, sortBy: 'label' } as const;

@Injectable({ providedIn: 'root' })
export class ReferentialService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  listSalesChannels(): Observable<SalesChannelRDto[]> {
    return this.listAll<SalesChannelRDto>('sales-channels');
  }

  listProductRanges(): Observable<ProductRangeRDto[]> {
    return this.listAll<ProductRangeRDto>('product-ranges');
  }

  listStockLocations(): Observable<StockLocationRDto[]> {
    return this.listAll<StockLocationRDto>('stock-locations');
  }

  private listAll<T>(resource: string): Observable<T[]> {
    return this.http
      .get<PageResponse<T>>(`${this.baseUrl}/${resource}`, { params: toHttpParams(ALL_ITEMS) })
      .pipe(map((page) => page.items));
  }
}
