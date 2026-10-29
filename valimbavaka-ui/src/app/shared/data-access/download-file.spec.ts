import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { filenameOf } from './download-file';

const reply = (disposition?: string): HttpResponse<Blob> =>
  new HttpResponse({
    body: new Blob(['x']),
    headers: disposition ? new HttpHeaders({ 'Content-Disposition': disposition }) : undefined,
  });

describe('filenameOf', () => {
  it('lit le nom annoncé par le serveur', () => {
    expect(filenameOf(reply('attachment; filename="articles.xlsx"'), 'defaut.xlsx')).toBe(
      'articles.xlsx',
    );
  });

  it('décode un nom encodé en UTF-8', () => {
    expect(filenameOf(reply("attachment; filename*=UTF-8''caf%C3%A9.xlsx"), 'defaut.xlsx')).toBe(
      'café.xlsx',
    );
  });

  it('retombe sur le nom par défaut sans en-tête', () => {
    expect(filenameOf(reply(), 'articles.xlsx')).toBe('articles.xlsx');
  });
});
