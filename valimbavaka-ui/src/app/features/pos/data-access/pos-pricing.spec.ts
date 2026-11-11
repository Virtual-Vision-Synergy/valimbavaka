import { PosPromotionKind, PosPromotionRDto, SaleChannel } from './pos-models';
import { PromotionLine, bestPromotion, changeDue, lineTotal, ticketTotals } from './pos-pricing';

const SAVON = 'savon';
const HUILE = 'huile';

const promotion = (
  kind: PosPromotionKind,
  value: number,
  overrides: Partial<PosPromotionRDto> = {},
): PosPromotionRDto => ({
  id: `promo-${kind}-${value}`,
  name: `Promo ${kind} ${value}`,
  kind,
  value,
  articleIds: [SAVON, HUILE],
  channels: ['SHOP', 'FACEBOOK', 'PHONE'],
  ...overrides,
});

const line = (articleId: string, unitPrice: number, quantity: number): PromotionLine => ({
  articleId,
  unitPrice,
  quantity,
  gross: lineTotal(unitPrice, quantity),
});

const discountOf = (kind: PosPromotionKind, value: number, lines: PromotionLine[]): number => {
  const applied = bestPromotion([promotion(kind, value)], 'SHOP', lines);
  return applied.lineDiscounts.reduce((sum, amount) => sum + amount, applied.ticketDiscount);
};

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

  it("arrondit la remise en pourcentage de chaque ligne à l'ariary (HALF_UP)", () => {
    expect(discountOf('PERCENT', 15, [line(SAVON, 8500, 3)])).toBe(3825);
    expect(discountOf('PERCENT', 15, [line(SAVON, 8500, 2), line(HUILE, 12000, 1)])).toBe(4350);
    expect(discountOf('PERCENT', 10, [line(SAVON, 335, 3)])).toBe(101);
    expect(discountOf('PERCENT', 12.5, [line(SAVON, 3333.33, 1.5)])).toBe(625);
  });

  it("borne la remise au montant de la ligne : le net n'est jamais négatif", () => {
    const lines = [line(SAVON, 8500, 1)];
    const discount = discountOf('PERCENT', 100, lines);
    expect(discount).toBe(8500);
    expect(ticketTotals([{ gross: 8500, discount }]).total).toBe(0);
  });

  it('retire un montant fixe une seule fois du ticket, borné au total des lignes visées', () => {
    const lines = [line(SAVON, 8500, 1), line(HUILE, 8500, 1)];
    expect(discountOf('AMOUNT', 5000, lines)).toBe(5000);
    expect(discountOf('AMOUNT', 20000, lines)).toBe(17000);
    expect(discountOf('AMOUNT', 0.5, lines)).toBe(1);
    const applied = bestPromotion([promotion('AMOUNT', 5000)], 'SHOP', lines);
    expect(applied.ticketDiscount).toBe(5000);
    expect(applied.lineDiscounts).toEqual([0, 0]);
  });

  it('offre une unité par groupe complet de N achetés (BUY_X)', () => {
    const buyThree = (quantity: number) => discountOf('BUY_X', 3, [line(SAVON, 8500, quantity)]);
    expect([2, 3, 5, 6, 7, 3.5, 2.999].map(buyThree)).toEqual([
      0, 8500, 8500, 17000, 17000, 8500, 0,
    ]);
  });

  it('ne remise que les articles visés par la promotion', () => {
    const targeted = promotion('PERCENT', 10, { articleIds: [HUILE] });
    const applied = bestPromotion([targeted], 'SHOP', [
      line(SAVON, 10000, 1),
      line(HUILE, 10000, 1),
    ]);
    expect(applied.lineDiscounts).toEqual([0, 1000]);
  });

  it('ignore une promotion qui ne vise pas le canal du poste', () => {
    const facebookOnly = promotion('PERCENT', 50, { channels: ['FACEBOOK'] });
    const channels: SaleChannel[] = ['SHOP', 'PHONE'];
    for (const channel of channels) {
      expect(bestPromotion([facebookOnly], channel, [line(SAVON, 8500, 2)]).promotion).toBeNull();
    }
    expect(bestPromotion([facebookOnly], 'FACEBOOK', [line(SAVON, 8500, 2)]).promotion).toBe(
      facebookOnly,
    );
  });

  it('retient la promotion la plus avantageuse sans cumul, la première en cas d’égalité', () => {
    const lines = [line(SAVON, 8500, 2)];
    const percent = promotion('PERCENT', 15);
    const amount = promotion('AMOUNT', 1000);
    expect(bestPromotion([amount, percent], 'SHOP', lines).promotion).toBe(percent);

    const first = promotion('AMOUNT', 1000, { id: 'premiere' });
    const second = promotion('AMOUNT', 1000, { id: 'seconde' });
    expect(bestPromotion([first, second], 'SHOP', lines).promotion).toBe(first);
  });

  it('retient le code promo quand il donne autant ou plus que la promotion automatique (VTE-04)', () => {
    const lines = [line(SAVON, 8500, 2)];
    const automatic = promotion('PERCENT', 10, { id: 'auto' });
    const better = promotion('PERCENT', 20, { id: 'code-plus' });
    const same = promotion('PERCENT', 10, { id: 'code-egal' });

    expect(bestPromotion([automatic], 'SHOP', lines, better).promotion).toBe(better);
    expect(bestPromotion([automatic], 'SHOP', lines, same).promotion).toBe(same);
  });

  it('garde la promotion automatique quand elle donne plus que le code, sans cumul (VTE-04)', () => {
    const lines = [line(SAVON, 8500, 2)];
    const automatic = promotion('PERCENT', 30, { id: 'auto' });
    const code = promotion('PERCENT', 20, { id: 'code' });

    const applied = bestPromotion([automatic], 'SHOP', lines, code);

    expect(applied.promotion).toBe(automatic);
    expect(applied.lineDiscounts).toEqual([5100]);
  });

  it("ignore un code sans effet ou d'un autre canal et garde l'automatique (VTE-04)", () => {
    const lines = [line(SAVON, 8500, 2)];
    const automatic = promotion('PERCENT', 10, { id: 'auto' });

    expect(bestPromotion([automatic], 'SHOP', lines, promotion('BUY_X', 3)).promotion).toBe(
      automatic,
    );
    expect(
      bestPromotion([automatic], 'SHOP', lines, promotion('PERCENT', 50, { channels: ['PHONE'] }))
        .promotion,
    ).toBe(automatic);
  });

  it('applique le code seul quand aucune promotion automatique ne vise le ticket (VTE-04)', () => {
    const code = promotion('AMOUNT', 2000, { id: 'code' });

    const applied = bestPromotion([], 'SHOP', [line(SAVON, 8500, 3)], code);

    expect(applied.promotion).toBe(code);
    expect(applied.ticketDiscount).toBe(2000);
  });

  it('ne retient aucune promotion pour un ticket vide ou sans effet', () => {
    expect(bestPromotion([promotion('PERCENT', 15)], 'SHOP', []).promotion).toBeNull();
    expect(
      bestPromotion([promotion('BUY_X', 3)], 'SHOP', [line(SAVON, 8500, 2)]).promotion,
    ).toBeNull();
  });

  it('ajoute la remise du ticket aux remises de ligne dans les totaux', () => {
    const totals = ticketTotals([{ gross: 17000, discount: 0 }], 5000);
    expect(totals).toEqual({ subtotal: 17000, discount: 5000, total: 12000 });
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
