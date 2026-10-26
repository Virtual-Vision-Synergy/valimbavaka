import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Route,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { AuthStore } from '../../core/auth/auth-store';
import { buildUser } from '../../core/auth/auth-testing';
import { ArticleSheetPage } from '../article/pages/article-sheet-page/article-sheet-page';
import { ARTICLES_ROUTES } from './articles.routes';

const routeAt = (path: string): Route | undefined =>
  ARTICLES_ROUTES.find((route) => route.path === path);

describe('ARTICLES_ROUTES', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  const canActivate = (path: string, role: 'ADMIN' | 'SELLER'): unknown => {
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    const guard = routeAt(path)?.canActivate?.[0] as CanActivateFn;
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  };

  it('should open the real article sheet on /articles/new, guarded by the create permission', async () => {
    const component = await (routeAt('new')?.loadComponent as () => Promise<unknown>)();

    expect(component).toBe(ArticleSheetPage);
    expect(canActivate('new', 'ADMIN')).toBe(true);
    expect(canActivate('new', 'SELLER')).toBeInstanceOf(UrlTree);
  });

  it('should not keep the provisional sheet/new route', () => {
    expect(routeAt('sheet/new')).toBeUndefined();
  });

  it('should guard the edit route with the update permission', async () => {
    const component = await (routeAt(':id/edit')?.loadComponent as () => Promise<unknown>)();

    expect(component).toBe(ArticleSheetPage);
    expect(canActivate(':id/edit', 'ADMIN')).toBe(true);
    expect(canActivate(':id/edit', 'SELLER')).toBeInstanceOf(UrlTree);
  });
});
