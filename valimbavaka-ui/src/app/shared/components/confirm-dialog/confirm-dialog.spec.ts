import { TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../core/i18n/i18n-testing';
import { ConfirmDialog } from './confirm-dialog';

describe('ConfirmDialog', () => {
  async function render(danger = false) {
    await TestBed.configureTestingModule({
      imports: [ConfirmDialog],
      providers: [provideTestTranslations()],
    }).compileComponents();
    const fixture = TestBed.createComponent(ConfirmDialog);
    fixture.componentRef.setInput('heading', 'Supprimer ?');
    fixture.componentRef.setInput('message', 'Action définitive.');
    fixture.componentRef.setInput('confirmLabel', 'Supprimer');
    fixture.componentRef.setInput('danger', danger);
    await fixture.whenStable();
    return fixture;
  }

  it('should emit confirmed when the confirmation button is pressed', async () => {
    const fixture = await render();
    const confirmed = vi.fn();
    fixture.componentInstance.confirmed.subscribe(confirmed);

    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');
    buttons[1].click();

    expect(confirmed).toHaveBeenCalledOnce();
  });

  it('should emit cancelled and not confirmed when cancelling', async () => {
    const fixture = await render();
    const confirmed = vi.fn();
    const cancelled = vi.fn();
    fixture.componentInstance.confirmed.subscribe(confirmed);
    fixture.componentInstance.cancelled.subscribe(cancelled);

    (fixture.nativeElement as HTMLElement).querySelectorAll('button')[0].click();

    expect(cancelled).toHaveBeenCalledOnce();
    expect(confirmed).not.toHaveBeenCalled();
  });

  it('should style the confirmation as destructive when danger is set', async () => {
    const fixture = await render(true);

    const confirmButton = (fixture.nativeElement as HTMLElement).querySelectorAll('button')[1];

    expect(confirmButton.classList.contains('btn-danger')).toBe(true);
  });
});
