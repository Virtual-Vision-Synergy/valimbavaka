import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { ARTICLES_FIXTURE, articleDetailFixture } from './article-fixtures';
import { ArticleDetailRDto, ArticleRDto } from './article-models';

/**
 * Catalogue d'articles (liste et détail 360°). Provisoire : données de démonstration ; à terme
 * `/api/v1/articles` (ART-05, ART-06). La création et la modification passent par `features/article`.
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
}
