import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { ArticleService } from '../../article/data-access/article-service';
import { StockRDto } from '../../stocks/data-access/stock-models';
import {
  toBatchWDto,
  toKpis,
  toLot,
  toMaterial,
  toOperator,
  toProducible,
} from './production-mapping';
import {
  LotRDto,
  LotWDto,
  ProductionBatchRDto,
  ProductionContextRDto,
  ProductionIndicatorsRDto,
  ProductionOperatorRDto,
} from './production-models';

/** Une page de 100 (taille maximale du serveur), triée par nom pour les sélecteurs. */
const BY_NAME: PageQuery = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' };
const RECENT_LOTS: PageQuery = { page: 1, size: 10 };
const OPERATORS: PageQuery = { page: 1, size: 100 };

/** Résultat d'une création : `lot` est absent si la saisie attend le réseau (file hors ligne). */
export interface CreatedLot {
  readonly queued: boolean;
  readonly lot?: LotRDto;
}

/**
 * Production (PRD-01 à PRD-04). Lectures par `HttpClient` : produits finis et entrants (ART-04), stock de
 * l'entrepôt (STK-01), vendeuses, lots récents et indicateurs du mois (PRD-04). Écritures par la file hors ligne
 * (SYS-07) : « Enregistrer » crée un lot planifié sans mouvement de stock ; « Valider » crée et valide le lot en une
 * seule requête, le serveur sortant les entrants et entrant le produit fini dans la même transaction (PRD-02).
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
      indicators: this.indicators(),
    }).pipe(
      map(({ outputs, inputs, stocks, operators, batches, indicators }) => {
        const products = outputs.items.map(toProducible);
        const options = operators.items.map(toOperator);
        return {
          nextNumber: '',
          operators: options,
          materials: inputs.items.map((article) => toMaterial(article, stocks.items)),
          products,
          recentLots: batches.items.map((batch) => toLot(batch, products, options)),
          kpis: toKpis(
            indicators,
            inputs.items.map((article) => article.id),
            stocks.items,
          ),
        };
      }),
    );
  }

  batches(query: PageQuery): Observable<PageResponse<ProductionBatchRDto>> {
    return this.http.get<PageResponse<ProductionBatchRDto>>(`${this.baseUrl}/batches`, {
      params: toHttpParams(query),
    });
  }

  /** Indicateurs du mois en cours (PRD-04), calculés par le serveur. */
  indicators(): Observable<ProductionIndicatorsRDto> {
    return this.http.get<ProductionIndicatorsRDto>(`${this.baseUrl}/indicators`);
  }

  getById(id: string): Observable<ProductionBatchRDto> {
    return this.http.get<ProductionBatchRDto>(`${this.baseUrl}/batches/${id}`);
  }

  operators(): Observable<PageResponse<ProductionOperatorRDto>> {
    return this.http.get<PageResponse<ProductionOperatorRDto>>(`${this.baseUrl}/operators`, {
      params: toHttpParams(OPERATORS),
    });
  }

  /**
   * `lot.validate` : création et validation en une seule requête (`production.complete`), donc rejouable
   * telle quelle hors ligne. `articleName` et `operator` servent à afficher le lot créé sans relire la liste.
   */
  createLot(
    lot: LotWDto,
    labels: Pick<LotRDto, 'articleName' | 'operator'>,
  ): Observable<CreatedLot> {
    const url = lot.validate ? `${this.baseUrl}/completed-batches` : `${this.baseUrl}/batches`;
    return this.outbox
      .submit<ProductionBatchRDto>('POST', url, toBatchWDto(lot))
      .pipe(map((result) => toCreatedLot(result, labels)));
  }

  /** Démarre un lot planifié (PRD-03) : statut « en cours », le stock ne bouge pas. */
  startLot(id: string): Observable<SubmitResult<ProductionBatchRDto>> {
    return this.outbox.submit<ProductionBatchRDto>('POST', `${this.baseUrl}/batches/${id}/start`);
  }

  /**
   * Termine (valide) un lot planifié ou en cours (PRD-02, PRD-03) : mouvements de stock et coûts figés côté
   * serveur.
   */
  completeLot(id: string): Observable<SubmitResult<ProductionBatchRDto>> {
    return this.outbox.submit<ProductionBatchRDto>(
      'POST',
      `${this.baseUrl}/batches/${id}/completion`,
    );
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
