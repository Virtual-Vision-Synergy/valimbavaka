import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { DayClosingRDto } from '../../data-access/day-models';
import { DayClosePage } from './day-close-page';

const URL = '/api/v1/days/closing';

const CLOSING: DayClosingRDto = {
  businessDate: '2026-11-11',
  status: 'OPEN',
  locations: [
    {
      locationId: 'store-id',
      code: 'STORE',
      label: 'Boutique',
      lines: [
        {
          articleId: 'soap',
          reference: 'ART-0031',
          name: 'Savon noir',
          opening: 4,
          transferred: 30,
          returned: 1,
          sold: 19,
          delivered: 0,
          salon: 0,
          shooting: 2,
          theoretical: 14,
          systemStock: 12,
          gap: -2,
        },
        {
          articleId: 'oil',
          reference: 'ART-0087',
          name: 'Huile de coco',
          opening: 0,
          transferred: 10,
          returned: 0,
          sold: 4,
          delivered: 0,
          salon: 0,
          shooting: 0,
          theoretical: 6,
          systemStock: 6,
          gap: 0,
        },
      ],
    },
    {
      locationId: 'delivery-id',
      code: 'DELIVERY',
      label: 'Livraison',
      lines: [],
    },
    { locationId: 'salon-id', code: 'TRADE_SHOW', label: 'Salon', lines: [] },
  ],
};

/** Réconciliation de clôture (JR-04) : les six composantes, le théorique, le compté et l'écart. */
describe('DayClosePage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<DayClosePage>;

  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [DayClosePage],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DayClosePage);
    fixture.detectChanges();
  };

  const host = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const answer = async (body: DayClosingRDto): Promise<void> => {
    httpMock.expectOne(URL).flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const cells = (row: number): string[] =>
    Array.from(host().querySelectorAll('tbody tr')[row].querySelectorAll('td')).map((cell) => {
      const input = cell.querySelector('input');
      return (input ? input.value : (cell.textContent ?? '')).replace(/\s+/g, ' ').trim();
    });

  afterEach(() => httpMock.verify());

  it('should show the six components, the theoretical, the count and the gap of each article', async () => {
    await open();

    await answer(CLOSING);

    const headers = Array.from(host().querySelectorAll('thead th')).map((th) =>
      (th.textContent ?? '').trim(),
    );
    expect(headers).toEqual(
      expect.arrayContaining([
        'Transféré',
        'Retour',
        'Vendu',
        'Livré',
        'Salon',
        'Shooting',
        'Théorique',
        'Compté',
        'Écart',
      ]),
    );
    // report, transféré, retour, vendu, livré, salon, shooting, théorique, compté, écart
    expect(cells(0).slice(0, 10)).toEqual(['4', '30', '1', '19', '0', '0', '2', '14', '12', '−2']);
    expect(cells(1).slice(0, 10)).toEqual(['0', '10', '0', '4', '0', '0', '0', '6', '6', '']);
  });

  it('should compute the gap again when the administrator changes the count', async () => {
    await open();
    await answer(CLOSING);
    const input = host().querySelectorAll('tbody tr')[1].querySelector('input')!;

    input.value = '5';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(cells(1).slice(7, 10)).toEqual(['6', '5', '−1']);
  });

  it('should list the three locations of the closing', async () => {
    await open();

    await answer(CLOSING);

    const chips = (host().textContent ?? '').replace(/\s+/g, ' ');
    expect(chips).toContain('Boutique');
    expect(chips).toContain('Livraison');
    expect(chips).toContain('Salon');
  });

  it('should tell the administrator that the day is not opened (409)', async () => {
    await open();

    httpMock
      .expectOne(URL)
      .flush(
        { code: 'CONFLICT', detail: "La journée du 11/11/2026 n'est pas ouverte." },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = host().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain("n'est pas ouverte");
    expect(host().querySelector('table')).toBeNull();
  });

  it('should have no detectable accessibility violation', async () => {
    await open();
    await answer(CLOSING);
    document.body.appendChild(host());

    const results = await axe.run(host(), {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(
      results.violations.map((violation) => `${violation.id}: ${violation.nodes[0]?.html}`),
    ).toEqual([]);
    host().remove();
  });
});
