import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { ArticleService } from '../../article/data-access/article-service';
import { StockRDto } from '../../stocks/data-access/stock-models';
import { toBatchWDto, toLot, toMaterial, toOperator, toProducible } from './production-mapping';
import {
  LotRDto,
  LotWDto,
  ProductionBatchRDto,
  ProductionContextRDto,
  ProductionKpisRDto,
  ProductionOperatorRDto,
} from './production-models';

/** Une page de 100 (taille maximale du serveur), triée par nom pour les sélecteurs. */
const BY_NAME: PageQuery = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' };
const RECENT_LOTS: PageQuery = { page: 1, size: 10 };
const OPERATORS: PageQuery = { page: 1, size: 100 };

/** Indicateurs encore à zéro : calculés par le serveur dans PRD-04. */
const NO_KPIS: ProductionKpisRDto = {
  lotsThisMonth: 0,
  curing: 0,
  unitsProduced: 0,
  unitsTrendPercent: 0,
  averageCost: 0,
  materialsToRebuy: 0,
};

/** Résultat d'une création : `lot` est absent si la saisie attend le réseau (file hors ligne). */
export interface CreatedLot {
  readonly queued: boolean;
  readonly lot?: LotRDto;
}

/**
 * Production (PRD-01). Lectures par `HttpClient` : produits finis et entrants (ART-04), stock de
 * l'entrepôt (STK-01), vendeuses et lots récents. Création par la file hors ligne (SYS-07) ; le serveur
 * contrôle chaque ligne et crée le lot planifié, sans mouvement de stock (PRD-02).
 */
@Injectable({ providedIn: 'root' })
export class ProductionService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly articles = inject(ArticleService);
  private readonly baseUrl = '/api/v1/production';

  context(): Observable<ProductionContextRDto> {
    return forkJoin({
      outputs: this.articles.listForProduction('OUTPUT', BY_NAME),
      inputs: this.articles.listForProduction('INPUT', BY_NAME),
      stocks: this.http.get<PageResponse<StockRDto>>('/api/v1/stocks', {
        params: toHttpParams(BY_NAME),
      }),
      operators: this.operators(),
      batches: this.batches(RECENT_LOTS),
    }).pipe(
      map(({ outputs, inputs, stocks, operators, batches }) => {
        const products = outputs.items.map(toProducible);
        const options = operators.items.map(toOperator);
        return {
          nextNumber: '',
          operators: options,
          materials: inputs.items.map((article) => toMaterial(article, stocks.items)),
          products,
          recentLots: batches.items.map((batch) => toLot(batch, products, options)),
          kpis: NO_KPIS,
        };
      }),
    );
  }

  batches(query: PageQuery): Observable<PageResponse<ProductionBatchRDto>> {
    return this.http.get<PageResponse<ProductionBatchRDto>>(`${this.baseUrl}/batches`, {
      params: toHttpParams(query),
    });
  }

  getById(id: string): Observable<ProductionBatchRDto> {
    return this.http.get<ProductionBatchRDto>(`${this.baseUrl}/batches/${id}`);
  }

  operators(): Observable<PageResponse<ProductionOperatorRDto>> {
    return this.http.get<PageResponse<ProductionOperatorRDto>>(`${this.baseUrl}/operators`, {
      params: toHttpParams(OPERATORS),
    });
  }

  /** `articleName` et `operator` servent à afficher le lot créé sans relire la liste. */
  createLot(
    lot: LotWDto,
    labels: Pick<LotRDto, 'articleName' | 'operator'>,
  ): Observable<CreatedLot> {
    return this.outbox
      .submit<ProductionBatchRDto>('POST', `${this.baseUrl}/batches`, toBatchWDto(lot))
      .pipe(map((result) => toCreatedLot(result, labels)));
  }
}

function toCreatedLot(
  result: SubmitResult<ProductionBatchRDto>,
  labels: Pick<LotRDto, 'articleName' | 'operator'>,
): CreatedLot {
  if (!result.body) {
    return { queued: true };
  }
  const lot = toLot(result.body, [], []);
  return { queued: result.queued, lot: { ...lot, ...labels } };
}
