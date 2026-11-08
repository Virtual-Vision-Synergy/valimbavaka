import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { ArticleService as ArticleApi } from '../../article/data-access/article-service';
import { ReferentialService } from '../../referential/data-access/referential-service';
import { ARTICLES_FIXTURE } from './article-fixtures';
import { ArticleRDto } from './article-models';
import { ArticleSheet, toArticleSheet } from './article-sheet';

/**
 * Catalogue d'articles. La liste reste sur des données de démonstration (ART-05) ; la fiche 360° (ART-06) lit
 * `GET /api/v1/articles/{id}/overview`. La création et la modification passent par `features/article`.
 */
@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly api = inject(ArticleApi);
  private readonly referential = inject(ReferentialService);

  list(): Observable<readonly ArticleRDto[]> {
    return demo(ARTICLES_FIXTURE);
  }

  /** Fiche 360° : un seul appel au serveur, plus les gammes du référentiel pour l'étiquette de gamme. */
  sheet(id: string): Observable<ArticleSheet> {
    return forkJoin({
      overview: this.api.overview(id),
      ranges: this.referential.listProductRanges(),
    }).pipe(map(({ overview, ranges }) => toArticleSheet(overview, ranges)));
  }
}
