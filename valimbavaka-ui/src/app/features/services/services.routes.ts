import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Salon et shooting : données de démonstration tant que les prestations (SRV-02) n'existent pas.
 * Catalogue des services salon (SRV-01) : lecture avec `services.read` (route parente), création et modification
 * réservées à l'administrateur (`services.create`, `services.update`) ; le serveur décide.
 */
export const SERVICES_ROUTES: Routes = [
  {
    path: '',
    title: 'services.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/services-page/services-page').then((m) => m.ServicesPage),
  },
  {
    path: 'catalog',
    title: 'salonCatalog.title',
    loadComponent: () =>
      import('./pages/salon-catalog-list-page/salon-catalog-list-page').then(
        (m) => m.SalonCatalogListPage,
      ),
  },
  {
    path: 'catalog/new',
    title: 'salonCatalog.form.newTitle',
    canActivate: [permissionGuard('services.create')],
    loadComponent: () =>
      import('./pages/salon-catalog-form-page/salon-catalog-form-page').then(
        (m) => m.SalonCatalogFormPage,
      ),
  },
  {
    path: 'catalog/:id/edit',
    title: 'salonCatalog.form.editTitle',
    canActivate: [permissionGuard('services.update')],
    loadComponent: () =>
      import('./pages/salon-catalog-form-page/salon-catalog-form-page').then(
        (m) => m.SalonCatalogFormPage,
      ),
  },
];
