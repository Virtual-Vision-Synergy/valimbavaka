import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SupplierRDto } from '../../../supplier/data-access/supplier-models';
import { huilesRavo, pageOf } from '../../pages/supplier-list-page/supplier-list-page.fixtures';
import { SupplierDetail } from './supplier-detail';

const SUPPLIER_URL = `/api/v1/suppliers/${huilesRavo.id}`;

/** Fiche latérale d'un fournisseur : coordonnées, articles fournis, activation, modification. */
describe('SupplierDetail', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SupplierDetail>;
  let element: HTMLElement;
  let changed: SupplierRDto[];

  const open = async (
    role: 'ADMIN' | 'SELLER',
    sheet: SupplierRDto = huilesRavo,
  ): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SupplierDetail],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SupplierDetail);
    fixture.componentRef.setInput('supplierId', huilesRavo.id);
    changed = [];
    fixture.componentInstance.changed.subscribe((updated) => changed.push(updated));
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock.expectOne(SUPPLIER_URL).flush(sheet);
    await settle();
  };

  const settle = async (): Promise<void> => {
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const button = (label: string): HTMLButtonElement | undefined =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (b) => b.textContent?.trim() === label,
    );

  const openArticlesTab = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('#supplier-tab-articles')?.click();
    await settle();
  };

  afterEach(() => httpMock.verify());

  it('should show the contact details read from the server', async () => {
    await open('ADMIN');

    expect(element.querySelector('h2')?.textContent).toContain('Huiles Ravo');
    expect(element.querySelector('dl')?.textContent).toContain('Rasoa Rakoto');
    expect(element.querySelector('a[href="tel:0341122233"]')).not.toBeNull();
    expect(element.querySelector('a[href="mailto:contact@fournisseur.mg"]')).not.toBeNull();
    expect(element.querySelector('dl')?.textContent).toContain('Livre le mardi');
  });

  it('should show a dash for every detail the sheet does not have', async () => {
    await open('ADMIN', {
      ...huilesRavo,
      contactName: null,
      phone: null,
      email: null,
      address: null,
      notes: null,
    });

    expect(element.querySelectorAll('dl dd')).toHaveLength(5);
    expect(element.querySelector('a[href^="tel:"]')).toBeNull();
    expect(Array.from(element.querySelectorAll('dl dd')).map((d) => d.textContent?.trim())).toEqual(
      ['—', '—', '—', '—', '—'],
    );
  });

  it('should link the edit button to the edit form of the supplier', async () => {
    await open('ADMIN');

    const link = element.querySelector<HTMLAnchorElement>('a.btn-icon');
    expect(link?.getAttribute('href')).toBe(`/suppliers/${huilesRavo.id}/edit`);
  });

  it('should hide the edit and activation buttons without the update permission', async () => {
    await open('SELLER');

    expect(element.querySelector('a.btn-icon')).toBeNull();
    expect(button('Désactiver')).toBeUndefined();
  });

  it('should deactivate the supplier with PATCH, show it inactive and tell the list', async () => {
    await open('ADMIN');

    button('Désactiver')?.click();
    const request = httpMock.expectOne(`${SUPPLIER_URL}/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush({ ...huilesRavo, active: false, version: 1 });
    await settle();

    expect(changed).toHaveLength(1);
    expect(element.querySelector('.badges')?.textContent).toContain('Inactif');
    expect(button('Activer')).toBeDefined();
  });

  it('should show the server message when the activation is refused', async () => {
    await open('ADMIN');

    button('Désactiver')?.click();
    httpMock
      .expectOne(`${SUPPLIER_URL}/status`)
      .flush(
        { title: 'Forbidden', status: 403, detail: 'Accès refusé', code: 'FORBIDDEN' },
        { status: 403, statusText: 'Forbidden' },
      );
    await settle();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Accès refusé');
    expect(changed).toHaveLength(0);
    expect(element.querySelector('.badges')?.textContent).toContain('Actif');
  });

  it('should read the articles of the supplier only when the tab is opened', async () => {
    await open('ADMIN');
    httpMock.expectNone((req) => req.url === '/api/v1/articles');

    await openArticlesTab();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/articles');
    expect(request.request.params.get('supplierId')).toBe(huilesRavo.id);
    request.flush({
      ...pageOf([]),
      items: [
        { id: 'a1', code: 'ART-0031', name: 'Huile de coco', cost: 4500 },
        { id: 'a2', code: 'ART-0032', name: 'Beurre de karité', cost: null },
      ],
    });
    await settle();
    expect(element.querySelector('.receptions')?.textContent).toContain('Huile de coco');
    expect(element.querySelector('.receptions')?.textContent).toContain('ART-0032');
    expect(element.querySelectorAll('.receptions li .num')[1].textContent?.trim()).toBe('—');
  });

  it('should say that the supplier provides no article when the list is empty', async () => {
    await open('ADMIN');

    await openArticlesTab();
    httpMock.expectOne((req) => req.url === '/api/v1/articles').flush(pageOf([]));
    await settle();

    expect(element.querySelector('.receptions')?.textContent).toContain('Aucun article fourni');
  });
});
