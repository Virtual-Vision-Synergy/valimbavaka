import { Injectable, signal } from '@angular/core';
import { ArticleType, ArticleUnit } from '../../article/data-access/article-models';

/** Valeur brute du formulaire d'article, conservée comme brouillon pendant la saisie. */
export interface ArticleDraftValue {
  readonly name: string;
  readonly rangeId: string;
  readonly barcode: string;
  readonly type: ArticleType;
  readonly price: number | null;
  readonly cost: number | null;
  readonly unit: ArticleUnit;
  readonly threshold: number | null;
  readonly supplierId: string | null;
}

/** Brouillon d'article en cours : survit à la navigation (mémoire de l'onglet, rien de sensible). */
export interface ArticleDraft {
  readonly value: ArticleDraftValue;
  readonly savedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ArticleDraftStore {
  private readonly current = signal<ArticleDraft | null>(null);

  readonly draft = this.current.asReadonly();

  save(value: ArticleDraftValue, savedAt: string): void {
    this.current.set({ value, savedAt });
  }

  clear(): void {
    this.current.set(null);
  }
}
