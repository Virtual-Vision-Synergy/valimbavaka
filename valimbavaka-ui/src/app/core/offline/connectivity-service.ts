import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';

/** État du réseau du navigateur, réactif (événements `online` / `offline`). */
@Injectable({ providedIn: 'root' })
export class ConnectivityService {
  private readonly online = signal(true);
  readonly isOnline = this.online.asReadonly();

  constructor() {
    const view = inject(DOCUMENT).defaultView;
    if (!view) {
      return;
    }
    this.online.set(view.navigator.onLine);
    const goOnline = (): void => this.online.set(true);
    const goOffline = (): void => this.online.set(false);
    view.addEventListener('online', goOnline);
    view.addEventListener('offline', goOffline);
    inject(DestroyRef).onDestroy(() => {
      view.removeEventListener('online', goOnline);
      view.removeEventListener('offline', goOffline);
    });
  }
}
