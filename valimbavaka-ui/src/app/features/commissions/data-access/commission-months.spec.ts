import { daysLeftInMonth, monthKey } from './commission-months';

describe('commission-months', () => {
  it("décale le mois en passant les changements d'année", () => {
    const date = new Date(2026, 0, 31);

    expect(monthKey(date, 0)).toBe('2026-01');
    expect(monthKey(date, -1)).toBe('2025-12');
    expect(monthKey(date, 13)).toBe('2027-02');
  });

  it("compte les jours restants du mois en cours, aujourd'hui compris", () => {
    expect(daysLeftInMonth('2026-11', new Date(2026, 10, 11))).toBe(20);
    expect(daysLeftInMonth('2026-11', new Date(2026, 10, 30))).toBe(1);
  });

  it('ne laisse aucun jour à un mois passé et tout le mois à un mois à venir', () => {
    expect(daysLeftInMonth('2026-10', new Date(2026, 10, 11))).toBe(0);
    expect(daysLeftInMonth('2026-12', new Date(2026, 10, 11))).toBe(31);
  });
});
