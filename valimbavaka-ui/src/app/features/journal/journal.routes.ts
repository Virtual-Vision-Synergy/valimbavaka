import { Routes } from '@angular/router';

export const JOURNAL_ROUTES: Routes = [
  {
    path: '',
    title: 'journal.title',
    loadComponent: () =>
      import('./pages/journal-list-page/journal-list-page').then((m) => m.JournalListPage),
  },
];
