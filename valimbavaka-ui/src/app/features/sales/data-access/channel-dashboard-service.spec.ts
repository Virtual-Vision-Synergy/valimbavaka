import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ChannelDashboardRDto } from './channel-dashboard-models';
import { ChannelDashboardService } from './channel-dashboard-service';

const URL = '/api/v1/sales/channel-dashboard';
const DASHBOARD: ChannelDashboardRDto = {
  from: '2026-11-01',
  to: '2026-11-30',
  totalRevenue: 100000,
  totalSalesCount: 7,
  channels: [],
};

describe('ChannelDashboardService', () => {
  let service: ChannelDashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ChannelDashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lit le tableau de bord de la période demandée', () => {
    service.get({ from: '2026-11-01', to: '2026-11-30' }).subscribe();

    const request = httpMock.expectOne((req) => req.url === URL);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('from')).toBe('2026-11-01');
    expect(request.request.params.get('to')).toBe('2026-11-30');
    request.flush(DASHBOARD);
  });

  it("n'envoie pas de période vide : le serveur prend le mois en cours", () => {
    service.get({ from: '', to: undefined }).subscribe();

    const request = httpMock.expectOne((req) => req.url === URL);
    expect(request.request.params.has('from')).toBe(false);
    expect(request.request.params.has('to')).toBe(false);
    request.flush(DASHBOARD);
  });
});
