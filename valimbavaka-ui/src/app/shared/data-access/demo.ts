import { Observable, delay, of } from 'rxjs';

/** Latence simulée : rend visibles les états de chargement tant que l'API n'existe pas. */
export const DEMO_LATENCY_MS = 350;

/**
 * Source provisoire des services sans backend : renvoie les données de démonstration avec une latence
 * réaliste. Le jour où l'API existe, le service remplace cet appel par `HttpClient`, sans changer l'appelant.
 */
export function demo<T>(value: T): Observable<T> {
  return of(value).pipe(delay(DEMO_LATENCY_MS));
}
