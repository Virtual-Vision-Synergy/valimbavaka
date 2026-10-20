import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';
import { OutboxService } from '../offline/outbox-service';
import { AuthRDto, LoginWDto } from './auth-models';
import { AuthStore } from './auth-store';

export const AUTH_URL_PREFIX = '/api/v1/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authStore = inject(AuthStore);
  private readonly outbox = inject(OutboxService);
  private inflightRefresh: Observable<AuthRDto> | null = null;

  login(credentials: LoginWDto): Observable<AuthRDto> {
    return this.http
      .post<AuthRDto>(`${AUTH_URL_PREFIX}/login`, credentials)
      .pipe(tap((response) => this.authStore.signIn(response.accessToken, response.user)));
  }

  /** Un seul refresh à la fois : les appels simultanés partagent la même requête (rotation du cookie). */
  refresh(): Observable<AuthRDto> {
    this.inflightRefresh ??= this.http.post<AuthRDto>(`${AUTH_URL_PREFIX}/refresh`, null).pipe(
      tap((response) => this.authStore.signIn(response.accessToken, response.user)),
      finalize(() => (this.inflightRefresh = null)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.inflightRefresh;
  }

  /** Restaure la session depuis le cookie de refresh (rechargement de page) ; false si absente ou expirée. */
  restoreSession(): Observable<boolean> {
    if (this.authStore.isAuthenticated()) {
      return of(true);
    }
    return this.refresh().pipe(
      map(() => true),
      catchError(() => of(false)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${AUTH_URL_PREFIX}/logout`, null).pipe(
      catchError(() => of(undefined)),
      finalize(() => {
        this.authStore.signOut();
        void this.outbox.clear();
      }),
    );
  }
}
