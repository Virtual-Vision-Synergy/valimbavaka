import { Injectable, signal } from '@angular/core';

export type ToastTone = 'success' | 'info' | 'warning' | 'danger';

/** Message éphémère : `messageKey` + `params` sont traduits à l'affichage. */
export interface Toast {
  readonly id: number;
  readonly messageKey: string;
  readonly params: Readonly<Record<string, string | number>>;
  readonly tone: ToastTone;
}

/** Durée d'affichage : assez longue pour être lue (WCAG 2.2.1), le message reste fermable à la main. */
const TOAST_DURATION_MS = 6000;

/** Retours d'action non bloquants (« Vente encaissée »…), annoncés aux lecteurs d'écran. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly items = signal<readonly Toast[]>([]);
  private nextId = 1;

  readonly toasts = this.items.asReadonly();

  show(
    messageKey: string,
    params: Readonly<Record<string, string | number>> = {},
    tone: ToastTone = 'success',
  ): void {
    const toast: Toast = { id: this.nextId++, messageKey, params, tone };
    this.items.update((list) => [...list.slice(-2), toast]);
    setTimeout(() => this.dismiss(toast.id), TOAST_DURATION_MS);
  }

  dismiss(id: number): void {
    this.items.update((list) => list.filter((toast) => toast.id !== id));
  }
}
