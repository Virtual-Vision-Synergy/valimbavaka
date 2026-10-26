import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { signal } from '@angular/core';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { type MockInstance, vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ConnectivityService } from '../../../../core/offline/connectivity-service';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ArticleFormPage } from './article-form-page';
import { RANGE_ID, SUPPLIER_ID } from './article-form-page.fixtures';

type Control = HTMLInputElement | HTMLSelectElement;

/** Connexion simulée : `online.set(false)` coupe le réseau, la saisie part alors en file hors ligne. */
export const online = signal(true);

/** Gestes de saisie sur le formulaire monté. */
interface Gestures {
  field<T extends Control>(id: string): T;
  submit(): HTMLButtonElement;
  type(id: string, value: string): Promise<void>;
  choose(name: string, label: string): Promise<void>;
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
  /** Clique la pastille dont le libellé contient `label` : la valeur d'un radio Angular n'est pas dans le DOM. */
  const clickRadio = (name: string, label: string): void =>
    Array.from(element.querySelectorAll<HTMLLabelElement>('label.choice'))
      .find(
        (choice) =>
          choice.querySelector(`input[formControlName="${name}"]`) &&
          choice.textContent?.includes(label),
      )
      ?.querySelector<HTMLInputElement>('input')
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
    choose: (name, label) => act(() => dom.clickRadio(name, label))(),
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
  online.set(true);
  await TestBed.configureTestingModule({
    imports: [ArticleFormPage],
    providers: [
      provideHttpClient(withInterceptors([errorInterceptor])),
      provideHttpClientTesting(),
      provideRouter([]),
      provideTestTranslations(),
      { provide: ConnectivityService, useValue: { isOnline: online.asReadonly() } },
    ],
  }).compileComponents();
  TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN')); // les écritures passent par la file hors ligne
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
