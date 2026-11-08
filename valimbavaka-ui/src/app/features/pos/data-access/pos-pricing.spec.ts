import { changeDue, lineDiscount, lineTotal, ticketTotals } from './pos-pricing';

/** Calcul du ticket au poste (VTE-02) : même règle que le serveur (`SaleLine.of`, VTE-01). */
describe('pos-pricing', () => {
  it("calcule le total d'une ligne comme le serveur (HALF_UP à 2 décimales)", () => {
    expect(lineTotal(8500, 3)).toBe(25500);
    expect(lineTotal(3333.33, 1.5)).toBe(5000);
    expect(lineTotal(0.33, 2.5)).toBe(0.83);
    expect(lineTotal(0.33, 1.5)).toBe(0.5);
  });

  it('additionne les lignes au centime près, sans erreur de virgule flottante', () => {
    expect(lineTotal(0.1, 3)).toBe(0.3);
    const totals = ticketTotals([
      { gross: 0.1, discount: 0 },
      { gross: 0.2, discount: 0 },
    ]);
    expect(totals).toEqual({ subtotal: 0.3, discount: 0, total: 0.3 });
  });

  it("arrondit la remise d'une ligne à l'ariary", () => {
    expect(lineDiscount(25500, 15)).toBe(3825);
    expect(lineDiscount(12345, 15)).toBe(1852);
    expect(lineDiscount(12500, 0)).toBe(0);
  });

  it("borne la remise au montant de la ligne : le net n'est jamais négatif", () => {
    const discount = lineDiscount(8500, 120);
    expect(discount).toBe(8500);
    expect(ticketTotals([{ gross: 8500, discount }]).total).toBe(0);
  });

  it('renvoie des totaux nuls pour un ticket vide', () => {
    expect(ticketTotals([])).toEqual({ subtotal: 0, discount: 0, total: 0 });
  });

  it('calcule le rendu monnaie et le manque au centime près', () => {
    expect(changeDue(60000, 58300)).toBe(1700);
    expect(changeDue(50000, 58300)).toBe(-8300);
    expect(changeDue(1, 0.7)).toBe(0.3);
  });
});
