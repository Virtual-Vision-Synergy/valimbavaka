import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { ConnectivityService } from '../../../core/offline/connectivity-service';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { PromotionDraft, PromotionRDto, PromotionReferences } from './promotion-models';
import { PromotionService } from './promotion-service';

const REFS: PromotionReferences = {
  ranges: [{ id: 'range-zezika', code: 'ZEZIKA', label: 'Zezika' }],
  channels: [{ id: 'channel-facebook', code: 'FACEBOOK', label: 'Facebook' }],
};

const DRAFT: PromotionDraft = {
  name: 'Fête des mères',
  kind: 'PERCENT',
  value: 20,
  code: null,
  scope: 'ZEZIKA',
  startsOn: '2026-11-09',
  endsOn: '2026-11-30',
  channels: ['FACEBOOK'],
  limit: null,
};

describe('PromotionService', () => {
  let service: PromotionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ConnectivityService, useValue: { isOnline: signal(true).asReadonly() } },
      ],
    });
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN')); // les écritures passent par la file hors ligne
    service = TestBed.inject(PromotionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('crée la promotion par POST avec une clé d’idempotence et les identifiants du référentiel', () => {
    let result: SubmitResult<PromotionRDto> | undefined;
    service.create(DRAFT, REFS).subscribe((value) => (result = value));

    const request = httpMock.expectOne('/api/v1/promotions');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    expect(request.request.body).toMatchObject({
      name: 'Fête des mères',
      scope: 'RANGES',
      targetIds: ['range-zezika'],
      channelIds: ['channel-facebook'],
      usageLimit: null,
    });
    request.flush({ id: 'promo-1' }, { status: 201, statusText: 'Created' });

    expect(result?.queued).toBe(false);
    expect(result?.body?.id).toBe('promo-1');
  });
});
