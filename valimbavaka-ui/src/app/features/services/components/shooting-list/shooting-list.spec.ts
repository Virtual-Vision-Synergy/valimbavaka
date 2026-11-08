import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ShootingRowRDto } from '../../data-access/service-models';
import { ShootingList } from './shooting-list';

const row = (number: string, status: ShootingRowRDto['status']): ShootingRowRDto => ({
  id: `id-${number}`,
  number,
  items: [{ name: 'Gommage café 200 g', quantity: 2 }],
  takenBy: 'Miora Andria',
  expectedReturn: '2026-11-08T16:00',
  status,
});

/** SHO-02 : bouton « Réintégrer » des sorties encore dehors, réservé à la permission shootings.return. */
describe('ShootingList', () => {
  let fixture: ComponentFixture<ShootingList>;
  let element: HTMLElement;
  let returned: string[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShootingList],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(ShootingList);
    fixture.componentRef.setInput('shootings', [
      row('SHO-0041', 'LATE'),
      row('SHO-0042', 'RETURNED'),
    ]);
    returned = [];
    fixture.componentInstance.returned.subscribe((number) => returned.push(number));
    element = fixture.nativeElement as HTMLElement;
  });

  it('propose Réintégrer pour la sortie encore dehors seulement et émet son numéro', () => {
    fixture.detectChanges();

    const buttons = element.querySelectorAll<HTMLButtonElement>('button');
    expect(buttons).toHaveLength(1);
    buttons[0]?.click();

    expect(returned).toEqual(['SHO-0041']);
  });

  it('masque Réintégrer sans la permission de réintégrer', () => {
    fixture.componentRef.setInput('canReturn', false);
    fixture.detectChanges();

    expect(element.querySelectorAll('button')).toHaveLength(0);
    expect(element.textContent).toContain('SHO-0041');
  });
});
