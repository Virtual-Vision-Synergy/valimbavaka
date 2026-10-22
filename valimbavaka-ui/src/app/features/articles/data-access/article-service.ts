import { Injectable } from '@angular/core';
import { Observable, map, throwError } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import {
  ARTICLES_FIXTURE,
  NEXT_REFERENCE,
  SUPPLIER_OPTIONS_FIXTURE,
  articleDetailFixture,
} from './article-fixtures';
import { ArticleDetailRDto, ArticleRDto, ArticleWDto, SupplierOptionRDto } from './article-models';

/**
 * Catalogue d'articles. Provisoire : données de démonstration ; à terme `/api/v1/articles`
 * (liste paginée, fiche, création), sans changer l'appelant.
 */
@Injectable({ providedIn: 'root' })
export class ArticleService {
  list(): Observable<readonly ArticleRDto[]> {
    return demo(ARTICLES_FIXTURE);
  }

  get(id: string): Observable<ArticleDetailRDto> {
    const detail = articleDetailFixture(id);
    return detail ? demo(detail) : throwError(() => new Error(`Article inconnu : ${id}`));
  }

  nextReference(): Observable<string> {
    return demo(NEXT_REFERENCE);
  }

  suppliers(): Observable<readonly SupplierOptionRDto[]> {
    return demo(SUPPLIER_OPTIONS_FIXTURE);
  }

  /** Simulation : renvoie la référence attribuée ; aucune donnée n'est conservée. */
  create(article: ArticleWDto): Observable<string> {
    return demo(article).pipe(map(() => NEXT_REFERENCE));
  }
}
