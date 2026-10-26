import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Catalogue d'articles : la liste et le détail restent sur des données de démonstration (ART-05, ART-06).
 * La création et la modification (ART-01, ART-02) utilisent le même formulaire, branché sur
 * `/api/v1/articles` (`features/article/data-access`).
 */
export const ARTICLES_ROUTES: Routes = [
  {
    path: '',
    title: 'articles.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/article-list-page/article-list-page').then((m) => m.ArticleListPage),
  },
  {
    path: 'new',
    title: 'article.form.newTitle',
    canActivate: [permissionGuard('articles.create')],
    loadComponent: () =>
      import('./pages/article-form-page/article-form-page').then((m) => m.ArticleFormPage),
  },
  {
    path: ':id/edit',
    title: 'article.form.editTitle',
    canActivate: [permissionGuard('articles.update')],
    loadComponent: () =>
      import('./pages/article-form-page/article-form-page').then((m) => m.ArticleFormPage),
  },
  {
    path: ':id',
    title: 'articles.detail.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/article-detail-page/article-detail-page').then((m) => m.ArticleDetailPage),
  },
];
