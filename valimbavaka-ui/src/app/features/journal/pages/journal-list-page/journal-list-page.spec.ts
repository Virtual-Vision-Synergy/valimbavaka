import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PageResponse } from '../../../../shared/models/page-response';
import { JournalEntryRDto } from '../../data-access/journal-models';
import { JournalListPage } from './journal-list-page';

const entry = (overrides: Partial<JournalEntryRDto> = {}): JournalEntryRDto => ({
  id: '0195f0a0-0000-7000-8000-0000000000a1',
  occurredAt: '2026-10-19T08:30:00Z',
  actorId: '0195f0a0-0000-7000-8000-000000000001',
  actorName: 'Rasoa Rakoto',
  action: 'CREATE',
  resourceType: 'USER',
  resourceId: '0195f0a0-0000-7000-8000-0000000000b1',
  description: "Création de l'utilisateur hanta@valimbavaka.mg",
  ...overrides,
});

const pageOf = (items: JournalEntryRDto[], totalPages = 1): PageResponse<JournalEntryRDto> => ({
  items,
  page: 1,
  pageSize: 20,
  totalCount: items.length,
  totalPages,
});

describe('JournalListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<JournalListPage>;
  let element: HTMLElement;

  const isJournalRequest = (req: { url: string }): boolean => req.url === '/api/v1/journal';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(JournalListPage);
    element = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => httpMock.verify());

  it('should show who did what, with a translated action', async () => {
    httpMock.expectOne(isJournalRequest).flush(pageOf([entry()]));
    await fixture.whenStable();

    const row = element.querySelector('tbody tr')?.textContent ?? '';
    expect(row).toContain('Rasoa Rakoto');
    expect(row).toContain('Création');
    expect(row).toContain('USER');
    expect(row).toContain("Création de l'utilisateur hanta@valimbavaka.mg");
    expect(element.querySelector('time')?.getAttribute('datetime')).toBe('2026-10-19T08:30:00Z');
  });

  it('should offer no way to edit or delete an entry', async () => {
    httpMock.expectOne(isJournalRequest).flush(pageOf([entry()]));
    await fixture.whenStable();

    expect(element.querySelector('tbody button, tbody a')).toBeNull();
  });

  it('should tell the administrator when the journal is empty', async () => {
    httpMock.expectOne(isJournalRequest).flush(pageOf([]));
    await fixture.whenStable();

    expect(element.querySelector('.empty')?.textContent).toContain('Aucune opération');
  });

  it('should search from the first page with the typed term', async () => {
    httpMock.expectOne(isJournalRequest).flush(pageOf([entry()], 3));
    await fixture.whenStable();

    const input = element.querySelector<HTMLInputElement>('#journal-search') as HTMLInputElement;
    input.value = ' Rakoto ';
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    const request = httpMock.expectOne(isJournalRequest);
    expect(request.request.params.get('search')).toBe('Rakoto');
    expect(request.request.params.get('page')).toBe('1');
    request.flush(pageOf([]));
  });

  it('should request the next page', async () => {
    httpMock.expectOne(isJournalRequest).flush(pageOf([entry()], 2));
    await fixture.whenStable();

    const next = element.querySelectorAll<HTMLButtonElement>('nav button')[1];
    next.click();

    const request = httpMock.expectOne(isJournalRequest);
    expect(request.request.params.get('page')).toBe('2');
    request.flush(pageOf([entry()], 2));
  });

  it('should display the error when the journal cannot be read', async () => {
    httpMock
      .expectOne(isJournalRequest)
      .flush(
        { status: 403, code: 'FORBIDDEN', detail: 'Accès refusé' },
        { status: 403, statusText: 'Forbidden' },
      );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')).not.toBeNull();
  });
});
