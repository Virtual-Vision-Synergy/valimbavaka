import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PromotionRDto } from '../../data-access/promotion-models';
import { PROMOTION_REFS, buildPromotion } from '../../data-access/promotion-testing';
import { PromotionDetail } from './promotion-detail';

/** Fiche détaillée d'une promotion (PRO-04), relue sur `GET /api/v1/promotions/{id}`. */
describe('PromotionDetail', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<PromotionDetail>;
  let element: HTMLElement;

  const open = async (sheet: PromotionRDto | null): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [PromotionDetail],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PromotionDetail);
    fixture.componentRef.setInput('promotionId', 'promo-1');
    fixture.componentRef.setInput('references', PROMOTION_REFS);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    const request = httpMock.expectOne('/api/v1/promotions/promo-1');
    if (sheet) {
      request.flush(sheet);
    } else {
      request.flush(
        { code: 'NOT_FOUND', detail: 'Promotion introuvable' },
        { status: 404, statusText: 'Not Found' },
      );
    }
    await fixture.whenStable();
    fixture.detectChanges();
  };

  afterEach(() => httpMock.verify());

  it('affiche la remise, le statut, la gamme et les canaux en libellés du référentiel', async () => {
    await open(buildPromotion());

    const text = element.textContent ?? '';
    expect(element.querySelector('h2')?.textContent).toContain('Fête des mères');
    expect(text).toContain('−20 %');
    expect(text).toContain('Active');
    expect(text).toContain('Zezika');
    expect(text).toContain('Facebook');
    expect(text).not.toContain('Boutique');
    expect(text).toContain('52 / 200');
    expect(text).toContain('Aucun (appliquée automatiquement)');
  });

  it('affiche le code, tout le catalogue et une utilisation illimitée', async () => {
    await open(
      buildPromotion({
        code: 'BF25',
        scope: 'ALL',
        targetIds: [],
        usageLimit: null,
        status: 'SCHEDULED',
      }),
    );

    const text = element.textContent ?? '';
    expect(text).toContain('BF25');
    expect(text).toContain('Tout le catalogue');
    expect(text).toContain('illimité');
    expect(text).toContain('Programmée');
  });

  it('propose de réessayer quand la promotion est introuvable', async () => {
    await open(null);

    expect(element.querySelector('h2')).toBeNull();
    expect(element.querySelector('vvs-list-state')).not.toBeNull();
  });
});
