import { TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../core/i18n/i18n-testing';
import { ChipOption, FilterChips } from './filter-chips';

const OPTIONS: readonly ChipOption<string>[] = [
  { value: 'ALL', label: 'Tous', count: 9 },
  { value: 'SELLER', label: 'Vendeuses', count: 5 },
];

describe('FilterChips', () => {
  it('should mark the selected chip as pressed and switch on click', async () => {
    await TestBed.configureTestingModule({
      imports: [FilterChips],
      providers: [provideTestTranslations()],
    }).compileComponents();
    const fixture = TestBed.createComponent(FilterChips<string>);
    fixture.componentRef.setInput('label', 'Rôle');
    fixture.componentRef.setInput('options', OPTIONS);
    fixture.componentRef.setInput('value', 'ALL');
    await fixture.whenStable();
    const chips = (fixture.nativeElement as HTMLElement).querySelectorAll('button');

    expect(chips[0].getAttribute('aria-pressed')).toBe('true');
    expect(chips[1].getAttribute('aria-pressed')).toBe('false');
    expect(chips[1].textContent).toContain('5');

    chips[1].click();
    await fixture.whenStable();

    expect(fixture.componentInstance.value()).toBe('SELLER');
  });
});
