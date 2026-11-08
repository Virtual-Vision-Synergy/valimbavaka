import { Injectable, computed, linkedSignal, signal } from '@angular/core';
import { InventorySheetLine, InventoryReason, InventorySheet } from './inventory-models';
import { gapOf, parseQuantity } from './inventory-sheet';

export type InventoryFilter = 'ALL' | 'TO_COUNT' | 'GAP' | 'NO_REASON';

export interface InventoryRow {
  readonly line: InventorySheetLine;
  /** Théorique du serveur, remplacé par celui figé au dernier comptage enregistré. */
  readonly theoretical: number;
  readonly counted: number | null;
  /** Écart compté − théorique ; null tant que l'article n'est pas compté. */
  readonly gap: number | null;
  readonly reason: InventoryReason | null;
}

/** Saisies de la session d'inventaire, dérivées de la session du serveur. */
@Injectable()
export class InventoryState {
  readonly session = signal<InventorySheet | undefined>(undefined);
  readonly filter = signal<InventoryFilter>('ALL');

  readonly counts = linkedSignal<Readonly<Record<string, number | null>>>(() =>
    Object.fromEntries((this.session()?.lines ?? []).map((line) => [line.articleId, line.counted])),
  );
  /** Théoriques figés par le serveur lors des comptages enregistrés depuis le chargement (INV-02). */
  readonly frozen = linkedSignal<InventorySheet | undefined, Readonly<Record<string, number>>>({
    source: this.session,
    computation: () => ({}),
  });
  readonly reasons = linkedSignal<Readonly<Record<string, InventoryReason | null>>>(() =>
    Object.fromEntries((this.session()?.lines ?? []).map((line) => [line.articleId, line.reason])),
  );

  readonly rows = computed<readonly InventoryRow[]>(() =>
    (this.session()?.lines ?? []).map((line) => {
      const counted = this.counts()[line.articleId] ?? null;
      const theoretical = this.frozen()[line.articleId] ?? line.theoretical;
      return {
        line,
        theoretical,
        counted,
        gap: counted === null ? null : gapOf(counted, theoretical),
        reason: this.reasons()[line.articleId] ?? null,
      };
    }),
  );
  readonly counted = computed(() => this.rows().filter((row) => row.counted !== null));
  readonly gapRows = computed(() => this.rows().filter((row) => row.gap !== null && row.gap !== 0));
  readonly missingReasons = computed(() => this.gapRows().filter((row) => !row.reason));
  readonly totals = computed(() => ({
    units: this.gapRows().reduce((sum, row) => sum + (row.gap ?? 0), 0),
    value: this.gapRows().reduce((sum, row) => sum + (row.gap ?? 0) * (row.line.unitCost ?? 0), 0),
  }));
  readonly visible = computed(() => {
    const filters: Record<InventoryFilter, (row: InventoryRow) => boolean> = {
      ALL: () => true,
      TO_COUNT: (row) => row.counted === null,
      GAP: (row) => row.gap !== null && row.gap !== 0,
      NO_REASON: (row) => row.gap !== null && row.gap !== 0 && !row.reason,
    };
    return this.rows().filter(filters[this.filter()]);
  });
  readonly canClose = computed(
    () =>
      this.rows().length > 0 &&
      this.counted().length === this.rows().length &&
      this.missingReasons().length === 0,
  );

  /** Saisie en cours : l'écart affiché suit immédiatement la quantité tapée (INV-02). */
  setCount(articleId: string, raw: string): void {
    const value = parseQuantity(raw);
    this.counts.update((all) => ({ ...all, [articleId]: value }));
  }

  /** Comptage enregistré par le serveur : son théorique figé remplace celui du chargement. */
  applyCount(articleId: string, theoretical: number): void {
    this.frozen.update((all) => ({ ...all, [articleId]: theoretical }));
  }

  setReason(articleId: string, reason: string): void {
    this.reasons.update((all) => ({
      ...all,
      [articleId]: (reason || null) as InventoryReason | null,
    }));
  }
}
