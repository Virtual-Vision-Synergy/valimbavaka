import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthStore } from '../../auth/auth-store';
import { buildUser } from '../../auth/auth-testing';
import { MainLayout } from './main-layout';

describe('MainLayout', () => {
  async function render(role: 'ADMIN' | 'SELLER' | 'DELIVERY_DRIVER'): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    const fixture = TestBed.createComponent(MainLayout);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const navText = (element: HTMLElement): string =>
    element.querySelector('nav[aria-label="Navigation principale"]')?.textContent ?? '';

  it('should show every module including users to the administrator', async () => {
    const element = await render('ADMIN');

    expect(navText(element)).toContain('Utilisateurs');
    expect(navText(element)).toContain('Production');
    expect(element.querySelector('main')).not.toBeNull();
  });

  it('should hide users and deliveries from a seller', async () => {
    const element = await render('SELLER');

    expect(navText(element)).toContain('Ventes');
    expect(navText(element)).not.toContain('Utilisateurs');
    expect(navText(element)).not.toContain('Livraisons');
  });

  it('should show only the dashboard and deliveries to a delivery driver', async () => {
    const element = await render('DELIVERY_DRIVER');

    expect(navText(element)).toContain('Livraisons');
    expect(navText(element)).toContain('Tableau de bord');
    expect(navText(element)).not.toContain('Ventes');
    expect(navText(element)).not.toContain('Utilisateurs');
  });

  it('should display the name and the role label of the signed in user with a logout button', async () => {
    const element = await render('SELLER');

    expect(element.textContent).toContain('Hanta Rasolofoson');
    expect(element.textContent).toContain('Vendeuse');
    expect(element.querySelector('button.logout')?.textContent).toContain('Déconnexion');
    TestBed.inject(HttpTestingController).verify();
  });
});
