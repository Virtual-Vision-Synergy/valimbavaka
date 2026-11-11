import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { DayLiveRDto } from '../../data-access/day-live-models';
import { DayLivePage, LIVE_REFRESH_MS } from './day-live-page';

const URL = '/api/v1/days/live';

const view = (overrides: Partial<DayLiveRDto> = {}): DayLiveRDto => ({
  businessDate: '2026-11-11',
  status: 'OPEN',
  generatedAt: '2026-11-11T10:00:00Z',
  sales: {
    revenue: 17000,
    count: 2,
    channels: [
      {
        salesChannelId: 'c-store',
        salesChannelLabel: 'Boutique',
        revenue: 17000,
        salesCount: 2,
        revenueSharePercent: 100,
      },
    ],
  },
  deliveries: { total: 4, toPrepare: 1, onRound: 1, delivered: 2, failed: 0, cancelled: 0 },
  services: { count: 1, revenue: 35000 },
  feed: [
    {
      type: 'SALE',
      id: 's1',
      reference: 'VTE-0002',
      label: 'Boutique',
      amount: 9000,
      status: 'VALIDATED',
      actorName: 'Miora Rakoto',
      occurredAt: '2026-11-11T09:30:00Z',
    },
    {
      type: 'SERVICE',
      id: 'p1',
      reference: 'PRE-0001',
      label: 'Soin du visage',
      amount: 35000,
      status: null,
      actorName: 'Miora Rakoto',
      occurredAt: '2026-11-11T08:00:00Z',
    },
  ],
  ...overrides,
});

/** Suivi en direct de la journée (JR-03) : indicateurs, flux des opérations, relecture automatique. */
describe('DayLivePage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<DayLivePage>;

  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [DayLivePage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DayLivePage);
    fixture.detectChanges();
    // la première lecture part au prochain tour d'horloge (timer(0))
    vi.advanceTimersByTime(0);
  };

  const text = (): string =>
    ((fixture.nativeElement as HTMLElement).textContent ?? '').replace(/\s+/g, ' ');

  const answer = (body: DayLiveRDto): void => {
    httpMock.expectOne(URL).flush(body);
    fixture.detectChanges();
  };

  beforeEach(() => vi.useFakeTimers());

  afterEach(() => {
    httpMock.verify();
    vi.useRealTimers();
  });

  it('should show the indicators and the feed of the day', async () => {
    await open();

    answer(view());

    expect(text()).toContain('Journée ouverte');
    expect(text()).toContain('17 000 Ar');
    expect(text()).toContain('2 vente(s) validée(s)');
    expect(text()).toContain('2 / 4');
    expect(text()).toContain('35 000 Ar');
    expect(text()).toContain('Boutique');
    const items = (fixture.nativeElement as HTMLElement).querySelectorAll('.feed-item');
    expect(items).toHaveLength(2);
    expect(items[0].textContent).toContain('VTE-0002');
    expect(items[1].textContent).toContain('PRE-0001');
  });

  it('should show that the day is not opened yet', async () => {
    await open();

    answer(view({ status: null, feed: [] }));

    expect(text()).toContain('Journée non ouverte');
    expect(text()).toContain('Aucune opération pour le moment');
  });

  it('should read the server again by itself, without manual reload', async () => {
    await open();
    answer(view());

    vi.advanceTimersByTime(LIVE_REFRESH_MS);
    answer(view({ sales: { revenue: 26000, count: 3, channels: [] } }));

    expect(text()).toContain('26 000 Ar');
    expect(text()).toContain('3 vente(s) validée(s)');
  });

  it('should keep the last view and warn when a refresh fails, then recover', async () => {
    await open();
    answer(view());

    vi.advanceTimersByTime(LIVE_REFRESH_MS);
    httpMock.expectOne(URL).flush('boom', { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    expect(text()).toContain('17 000 Ar');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();

    vi.advanceTimersByTime(LIVE_REFRESH_MS);
    answer(view());

    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('should not stack requests while one is still running', async () => {
    await open();

    vi.advanceTimersByTime(LIVE_REFRESH_MS * 2);

    const pending = httpMock.match(URL);
    expect(pending).toHaveLength(1);
    pending[0].flush(view());
  });

  it('should read the server at once when the administrator presses refresh', async () => {
    await open();
    answer(view());

    const button = (fixture.nativeElement as HTMLElement).querySelector('button.btn-secondary');
    (button as HTMLButtonElement).click();
    answer(view({ sales: { revenue: 40000, count: 5, channels: [] } }));

    expect(text()).toContain('40 000 Ar');
  });

  it('should offer a retry when the very first read fails', async () => {
    await open();

    httpMock.expectOne(URL).flush('boom', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(text()).toContain('Chargement impossible');
  });
});
