import { HttpParams } from '@angular/common/http';
import { PageQuery } from './page-response';

/** Convertit une PageQuery en paramètres d'URL, sans envoyer les valeurs vides. */
export function toHttpParams(query: PageQuery): HttpParams {
  let params = new HttpParams().set('page', query.page).set('size', query.size);
  for (const key of ['search', 'sortBy', 'sortDirection'] as const) {
    const value = query[key];
    if (value) {
      params = params.set(key, value);
    }
  }
  return params;
}
