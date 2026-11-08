import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { StockRDto } from '../../stocks/data-access/stock-models';
import { SalonServiceListQuery } from './salon-catalog-models';
import { SalonCatalogService } from './salon-catalog-service';
import { ServicesContextRDto } from './service-models';
import {
  isSameLocalDay,
  toSalonProduct,
  toServiceRow,
  toTreatment,
} from './service-record-mapping';
import { ServiceRecordService } from './service-record-service';

const ACTIVE_SERVICES: SalonServiceListQuery = {
  page: 1,
  size: 100,
  active: true,
  sortBy: 'name',
  sortDirection: 'ASC',
};
const ALL_ARTICLES: PageQuery = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' };
const LATEST_RECORDS: PageQuery = {
  page: 1,
  size: 100,
  sortBy: 'createdAt',
  sortDirection: 'DESC',
};

/**
 * Salon : lit l'API (SRV-02) : services actifs du catalogue, stock salon de chaque article actif et prestations du
 * jour (les 100 plus récentes). Les sorties shooting passent par `ShootingService` (SHO-01 à SHO-03).
 */
@Injectable({ providedIn: 'root' })
export class SalonService {
  private readonly http = inject(HttpClient);
  private readonly catalog = inject(SalonCatalogService);
  private readonly records = inject(ServiceRecordService);

  context(): Observable<ServicesContextRDto> {
    return forkJoin({
      services: this.catalog.list(ACTIVE_SERVICES),
      stocks: this.http.get<PageResponse<StockRDto>>('/api/v1/stocks', {
        params: toHttpParams(ALL_ARTICLES),
      }),
      records: this.records.list(LATEST_RECORDS),
    }).pipe(
      map(({ services, stocks, records }) => {
        const products = stocks.items.map(toSalonProduct);
        const known = new Set(products.map((product) => product.id));
        const now = new Date();
        return {
          treatments: services.items.map((service) => toTreatment(service, known)),
          products,
          today: records.items
            .filter((record) => isSameLocalDay(record.performedAt, now))
            .map(toServiceRow),
        };
      }),
    );
  }
}
