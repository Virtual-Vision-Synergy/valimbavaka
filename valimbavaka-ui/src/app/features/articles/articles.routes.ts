import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Catalogue d'articles : liste, formulaire de création et détail restent sur des données de démonstration.
 * La fiche réelle (ART-01, API `/api/v1/articles`) est servie par `features/article`.
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
    title: 'articles.form.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/article-form-page/article-form-page').then((m) => m.ArticleFormPage),
  },
  {
    path: 'sheet/new',
    title: 'article.form.newTitle',
    canActivate: [permissionGuard('articles.create')],
    loadComponent: () =>
      import('../article/pages/article-sheet-page/article-sheet-page').then(
        (m) => m.ArticleSheetPage,
      ),
  },
  {
    path: ':id/edit',
    title: 'article.form.editTitle',
    canActivate: [permissionGuard('articles.update')],
    loadComponent: () =>
      import('../article/pages/article-sheet-page/article-sheet-page').then(
        (m) => m.ArticleSheetPage,
      ),
  },
  {
    path: ':id',
    title: 'articles.detail.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/article-detail-page/article-detail-page').then((m) => m.ArticleDetailPage),
  },
];
