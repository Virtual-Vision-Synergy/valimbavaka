import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { InvoiceDelivery } from './invoice-delivery';

describe('InvoiceDelivery', () => {
  let service: InvoiceDelivery;

  beforeEach(() => {
    vi.restoreAllMocks();
    service = TestBed.inject(InvoiceDelivery);
  });

  it('enregistre le fichier par un lien de téléchargement nommé puis libère son adresse', () => {
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:facture'),
      revokeObjectURL: vi.fn(),
    });
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    service.save(new Blob(['%PDF']), 'FAC-2026-0042.pdf');

    expect(click).toHaveBeenCalledOnce();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:facture');
    vi.unstubAllGlobals();
  });

  it('ne propose pas le partage quand le navigateur ne sait pas partager de fichier', () => {
    vi.stubGlobal('navigator', {});

    expect(service.canShare(new File(['%PDF'], 'f.pdf'))).toBe(false);
    vi.unstubAllGlobals();
  });

  it('propose le partage quand le navigateur accepte le fichier', () => {
    vi.stubGlobal('navigator', { canShare: () => true });

    expect(service.canShare(new File(['%PDF'], 'f.pdf'))).toBe(true);
    vi.unstubAllGlobals();
  });

  it('transmet le fichier, le titre et le texte au menu de partage', async () => {
    const share = vi.fn(() => Promise.resolve());
    vi.stubGlobal('navigator', { share });
    const file = new File(['%PDF'], 'f.pdf');

    await service.share(file, 'Facture', 'Bonjour');

    expect(share).toHaveBeenCalledWith({ files: [file], title: 'Facture', text: 'Bonjour' });
    vi.unstubAllGlobals();
  });
});
