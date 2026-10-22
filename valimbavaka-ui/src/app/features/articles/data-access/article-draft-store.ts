import { Injectable, signal } from '@angular/core';
import { ArticleFormValue } from './article-models';

/** Brouillon d'article en cours : survit à la navigation (mémoire de l'onglet, rien de sensible). */
export interface ArticleDraft {
  readonly value: ArticleFormValue;
  readonly savedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ArticleDraftStore {
  private readonly current = signal<ArticleDraft | null>(null);

  readonly draft = this.current.asReadonly();

  save(value: ArticleFormValue, savedAt: string): void {
    this.current.set({ value, savedAt });
  }

  clear(): void {
    this.current.set(null);
  }
}
