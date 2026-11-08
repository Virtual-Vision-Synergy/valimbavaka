import { PromotionDraft, PromotionRDto, PromotionReferences } from './promotion-models';
import { toCardView, toPromotionWDto } from './promotion-request';

const REFS: PromotionReferences = {
  ranges: [
    { id: 'range-botanika', code: 'BOTANIKA', label: 'Botanika' },
    { id: 'range-zezika', code: 'ZEZIKA', label: 'Zezika' },
  ],
  channels: [
    { id: 'channel-boutique', code: 'BOUTIQUE', label: 'Boutique' },
    { id: 'channel-facebook', code: 'FACEBOOK', label: 'Facebook' },
    { id: 'channel-telephone', code: 'TELEPHONE', label: 'Téléphone' },
    { id: 'channel-livraison', code: 'LIVRAISON', label: 'Livraison' },
  ],
};

const DRAFT: PromotionDraft = {
  name: 'Octobre rose',
  kind: 'PERCENT',
  value: 15,
  code: 'ROSE15',
  scope: 'BOTANIKA',
  startsOn: '2026-11-09',
  endsOn: '2026-12-09',
  channels: ['SHOP', 'PHONE'],
  limit: 100,
};

describe('toPromotionWDto', () => {
  it('cible la gamme choisie et traduit les canaux en identifiants du référentiel', () => {
    const body = toPromotionWDto(DRAFT, REFS);

    expect(body.scope).toBe('RANGES');
    expect(body.targetIds).toEqual(['range-botanika']);
    expect(body.channelIds).toEqual(['channel-boutique', 'channel-telephone']);
    expect(body.usageLimit).toBe(100);
    expect(body.code).toBe('ROSE15');
  });

  it("n'envoie aucune cible pour tout le catalogue", () => {
    const body = toPromotionWDto({ ...DRAFT, scope: 'ALL', code: null }, REFS);

    expect(body.scope).toBe('ALL');
    expect(body.targetIds).toEqual([]);
    expect(body.code).toBeNull();
  });

  it("n'envoie pas de cible inconnue quand la gamme manque au référentiel (le serveur répond 422)", () => {
    const body = toPromotionWDto({ ...DRAFT, scope: 'SOLIKA' }, REFS);

    expect(body.scope).toBe('RANGES');
    expect(body.targetIds).toEqual([]);
  });
});

describe('toCardView', () => {
  it('affiche le compteur et la limite de la promotion créée', () => {
    const created: PromotionRDto = {
      id: 'promo-1',
      name: 'Octobre rose',
      description: null,
      kind: 'PERCENT',
      value: 15,
      code: 'ROSE15',
      scope: 'RANGES',
      targetIds: ['range-botanika'],
      startsOn: '2026-11-09',
      endsOn: '2026-12-09',
      channelIds: ['channel-boutique'],
      usageLimit: 100,
      usageCount: 0,
      status: 'SCHEDULED',
      createdAt: '2026-11-08T06:00:00Z',
      updatedAt: '2026-11-08T06:00:00Z',
      version: 0,
    };

    expect(toCardView(created)).toMatchObject({
      description: '',
      uses: 0,
      limit: 100,
      discountGranted: 0,
    });
  });
});
