import { ShootingRDto } from './service-models';
import { displayStatus, toLocalTimestamp, toShootingRow } from './shooting-mapping';

const shooting = (overrides: Partial<ShootingRDto>): ShootingRDto => ({
  id: 'sho-1',
  number: 'SHO-0042',
  locationId: 'loc-store',
  items: [
    { articleId: 'art-1', articleCode: 'ART-0031', articleName: 'Gommage café 200 g', quantity: 1 },
    {
      articleId: 'art-2',
      articleCode: 'ART-0007',
      articleName: 'Baume lèvres miel 15 g',
      quantity: 2.5,
    },
  ],
  takenById: 'user-1',
  takenBy: 'Miora Andria',
  expectedReturnAt: '2026-11-08T13:00:00Z',
  note: null,
  status: 'OUT',
  late: false,
  createdAt: '2026-11-08T07:00:00Z',
  returnedAt: null,
  returnedBy: null,
  ...overrides,
});

/** SHO-01 : la sortie du serveur devient une ligne de `ShootingList` (statut Sorti, En retard ou Réintégré). */
describe('shooting-mapping', () => {
  it('affiche une sortie dans les temps avec le statut Sorti', () => {
    expect(displayStatus(shooting({}))).toBe('OUT');
  });

  it('affiche une sortie encore dehors après le retour prévu comme En retard', () => {
    expect(displayStatus(shooting({ late: true }))).toBe('LATE');
  });

  it('affiche une sortie réintégrée comme Réintégré, même si elle était en retard', () => {
    expect(displayStatus(shooting({ status: 'RETURNED', late: true }))).toBe('RETURNED');
  });

  it('reprend identifiant, numéro, auteur, articles et quantités, et le retour prévu en heure locale', () => {
    const row = toShootingRow(shooting({}));

    expect(row.id).toBe('sho-1');
    expect(row.number).toBe('SHO-0042');
    expect(row.takenBy).toBe('Miora Andria');
    expect(row.items).toEqual([
      { name: 'Gommage café 200 g', quantity: 1 },
      { name: 'Baume lèvres miel 15 g', quantity: 2.5 },
    ]);
    expect(row.expectedReturn).toBe(toLocalTimestamp(new Date('2026-11-08T13:00:00Z')));
    expect(row.expectedReturn).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
  });

  it('formate une date locale en AAAA-MM-JJTHH:MM, avec les zéros', () => {
    expect(toLocalTimestamp(new Date(2026, 0, 5, 9, 7))).toBe('2026-01-05T09:07');
  });
});
