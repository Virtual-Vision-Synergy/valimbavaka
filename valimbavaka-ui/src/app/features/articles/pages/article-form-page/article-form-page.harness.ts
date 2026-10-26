import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { type MockInstance, vi } from 'vitest';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ArticleFormPage } from './article-form-page';
import { RANGE_ID, SUPPLIER_ID } from './article-form-page.fixtures';

type Control = HTMLInputElement | HTMLSelectElement;

/** Gestes de saisie sur le formulaire monté. */
interface Gestures {
  field<T extends Control>(id: string): T;
  submit(): HTMLButtonElement;
  type(id: string, value: string): Promise<void>;
  choose(name: string, value: string): Promise<void>;
  pick(id: string, label: string): Promise<void>;
  press(label: string): Promise<void>;
  save(): Promise<void>;
}

/** Formulaire monté pour les tests : requêtes HTTP simulées et gestes de saisie. */
export interface FormHarness extends Gestures {
  readonly fixture: ComponentFixture<ArticleFormPage>;
  readonly element: HTMLElement;
  readonly httpMock: HttpTestingController;
  readonly navigate: MockInstance<Router['navigate']>;
  readonly toast: ToastService;
  flushChoices(): void;
}

const pageOf = (items: object[]) => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount: items.length,
  totalPages: 1,
});

/** Recherches et clics bruts dans le DOM du formulaire. */
function domOf(element: HTMLElement) {
  const field = <T extends Control>(id: string): T => element.querySelector<T>(`#${id}`) as T;
  const submit = (): HTMLButtonElement =>
    element.querySelector<HTMLButtonElement>('button[type="submit"]') as HTMLButtonElement;
  const clickButton = (label: string): void =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'))
      .find((button) => button.textContent?.trim() === label)
      ?.click();
  const clickRadio = (name: string, value: string): void =>
    Array.from(
      element.querySelectorAll<HTMLInputElement>(`input[type="radio"][formControlName="${name}"]`),
    )
      .find((radio) => radio.value === value)
      ?.click();
  return { field, submit, clickButton, clickRadio };
}

function gesturesOn(fixture: ComponentFixture<ArticleFormPage>): Gestures {
  const dom = domOf(fixture.nativeElement as HTMLElement);
  const settle = async (): Promise<void> => {
    await fixture.whenStable();
    fixture.detectChanges();
  };
  const act = (gesture: () => void) => async (): Promise<void> => {
    gesture();
    await settle();
  };

  return {
    field: dom.field,
    submit: dom.submit,
    type: async (id, value) => {
      const control = dom.field(id);
      control.value = value;
      const kind = control instanceof HTMLSelectElement ? 'change' : 'input';
      await act(() => control.dispatchEvent(new Event(kind)))();
    },
    choose: (name, value) => act(() => dom.clickRadio(name, value))(),
    pick: async (id, label) => {
      const select = dom.field<HTMLSelectElement>(id);
      const option = Array.from(select.options).find((o) => o.textContent?.trim() === label);
      select.value = option?.value ?? '';
      await act(() => select.dispatchEvent(new Event('change')))();
    },
    press: (label) => act(() => dom.clickButton(label))(),
    save: act(() => dom.submit().click()),
  };
}

function flushChoicesOn(httpMock: HttpTestingController): void {
  httpMock
    .expectOne((req) => req.url === '/api/v1/product-ranges')
    .flush(pageOf([{ id: RANGE_ID, code: 'BOTANIKA', label: 'Botanika' }]));
  httpMock
    .expectOne((req) => req.url === '/api/v1/suppliers')
    .flush(
      pageOf([
        { id: SUPPLIER_ID, name: 'Huiles Ravo', active: true },
        { id: 'inactive', name: 'Ancien fournisseur', active: false },
      ]),
    );
}

/** Monte la page ; `id` donné = modification, absent = création. */
export async function mountForm(id?: string): Promise<FormHarness> {
  await TestBed.configureTestingModule({
    imports: [ArticleFormPage],
    providers: [
      provideHttpClient(withInterceptors([errorInterceptor])),
      provideHttpClientTesting(),
      provideRouter([]),
      provideTestTranslations(),
    ],
  }).compileComponents();
  const httpMock = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(ArticleFormPage);
  if (id) {
    fixture.componentRef.setInput('id', id);
  }
  fixture.detectChanges();
  return {
    ...gesturesOn(fixture),
    fixture,
    element: fixture.nativeElement as HTMLElement,
    httpMock,
    navigate: vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true),
    toast: TestBed.inject(ToastService),
    flushChoices: () => flushChoicesOn(httpMock),
  };
}
