import { Routes } from '@angular/router';

/** Catalogue d'articles : données de démonstration tant que le module backend n'existe pas. */
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
    path: ':id',
    title: 'articles.detail.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/article-detail-page/article-detail-page').then((m) => m.ArticleDetailPage),
  },
];
