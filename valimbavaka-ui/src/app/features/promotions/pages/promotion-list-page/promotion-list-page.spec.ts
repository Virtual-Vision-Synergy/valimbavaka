import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PROMOTION_REFS, buildPromotion, pageOf } from '../../data-access/promotion-testing';
import { PromotionListPage } from './promotion-list-page';

const PROMOTIONS_URL = '/api/v1/promotions';
const ACTIVE = buildPromotion();
const ENDED = buildPromotion({
  id: 'promo-2',
  name: 'Rentrée',
  status: 'ENDED',
  startsOn: '2026-09-01',
  endsOn: '2026-09-30',
});

/** Liste des promotions branchée sur l'API (PRO-04) : filtre par statut serveur, fiche au clic. */
describe('PromotionListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<PromotionListPage>;
  let element: HTMLElement;

  const settle = async (): Promise<void> => {
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** Un geste qui relance une requête : on n'attend pas `whenStable()` tant qu'elle n'a pas répondu. */
  const refresh = async (): Promise<void> => {
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
  };

  const nextList = (): TestRequest => httpMock.expectOne((req) => req.url === PROMOTIONS_URL);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromotionListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PromotionListPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock
      .expectOne((req) => req.url.endsWith('/sales-channels'))
      .flush({ ...pageOf([]), items: PROMOTION_REFS.channels });
    httpMock
      .expectOne((req) => req.url.endsWith('/product-ranges'))
      .flush({ ...pageOf([]), items: PROMOTION_REFS.ranges });
  });

  afterEach(() => httpMock.verify());

  it('affiche les promotions renvoyées par le serveur avec leur statut', async () => {
    const request = nextList();
    expect(request.request.params.has('status')).toBe(false);
    request.flush(pageOf([ACTIVE, ENDED]));
    await settle();

    const titles = Array.from(element.querySelectorAll('vvs-promotion-card h3')).map((title) =>
      title.textContent?.trim(),
    );
    expect(titles).toEqual(['Fête des mères', 'Rentrée']);
    expect(element.querySelector('[data-status="ENDED"]')).not.toBeNull();
  });

  it('demande au serveur les seules promotions terminées quand on choisit ce filtre', async () => {
    nextList().flush(pageOf([ACTIVE, ENDED]));
    await settle();

    Array.from(element.querySelectorAll<HTMLButtonElement>('.chips button'))
      .find((chip) => chip.textContent?.includes('Terminées'))
      ?.click();
    await refresh();

    const request = nextList();
    expect(request.request.params.get('status')).toBe('ENDED');
    expect(request.request.params.get('page')).toBe('1');
    request.flush(pageOf([ENDED]));
    await settle();
    expect(element.querySelectorAll('vvs-promotion-card')).toHaveLength(1);
  });

  it('ouvre la fiche détaillée au clic sur une promotion', async () => {
    nextList().flush(pageOf([ACTIVE, ENDED]));
    await settle();

    element.querySelectorAll<HTMLButtonElement>('vvs-promotion-card button.open')[1]?.click();
    await refresh();

    httpMock.expectOne(`${PROMOTIONS_URL}/promo-2`).flush(ENDED);
    await settle();
    const sheet = element.querySelector('vvs-promotion-detail');
    expect(sheet?.querySelector('h2')?.textContent).toContain('Rentrée');
    expect(
      element.querySelectorAll('vvs-promotion-card button.open')[1]?.getAttribute('aria-pressed'),
    ).toBe('true');
  });

  it('affiche l’état vide quand aucune promotion ne correspond', async () => {
    nextList().flush(pageOf([]));
    await settle();

    expect(element.textContent).toContain('Aucune promotion dans ce filtre');
  });
});
