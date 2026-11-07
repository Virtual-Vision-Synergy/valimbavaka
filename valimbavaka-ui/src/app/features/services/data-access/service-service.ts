import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { StockRDto } from '../../stocks/data-access/stock-models';
import { SalonServiceListQuery } from './salon-catalog-models';
import { SalonCatalogService } from './salon-catalog-service';
import { SERVICES_FIXTURE } from './service-fixtures';
import { ServicesContextRDto } from './service-models';
import { toSalonProduct, toServiceRow, toTreatment } from './service-record-mapping';
import { ServiceRecordDayQuery } from './service-record-models';
import { ServiceRecordService } from './service-record-service';

const ACTIVE_SERVICES: SalonServiceListQuery = {
  page: 1,
  size: 100,
  active: true,
  sortBy: 'name',
  sortDirection: 'ASC',
};
const ALL_ARTICLES: PageQuery = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' };
const MY_DAY: ServiceRecordDayQuery = {
  page: 1,
  size: 100,
  sortBy: 'createdAt',
  sortDirection: 'DESC',
};

/**
 * Salon et shooting. Le salon lit l'API : services actifs du catalogue, stock salon de chaque article actif (SRV-02)
 * et prestations du jour du compte connecté, filtrées par le serveur (SRV-03, les 100 plus récentes). Les sorties
 * shooting restent en démonstration (SHO-01 à SHO-03).
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
      records: this.records.mine(MY_DAY),
    }).pipe(
      map(({ services, stocks, records }) => {
        const products = stocks.items.map(toSalonProduct);
        const units = new Map(products.map((product) => [product.id, product.unit]));
        const known = new Set(units.keys());
        return {
          treatments: services.items.map((service) => toTreatment(service, known)),
          products,
          today: records.items.map((record) => toServiceRow(record, units)),
          shootings: SERVICES_FIXTURE.shootings,
        };
      }),
    );
  }

  /** Simulation : réintègre les produits d'un shooting au stock. */
  returnShooting(number: string): Observable<string> {
    return demo(number).pipe(map((value) => value));
  }
}
