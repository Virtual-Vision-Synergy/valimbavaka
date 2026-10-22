import { Injectable, computed, linkedSignal, signal } from '@angular/core';
import {
  InventoryCloseWDto,
  InventoryLineRDto,
  InventoryReason,
  InventorySessionRDto,
} from './inventory-models';

export type InventoryFilter = 'ALL' | 'TO_COUNT' | 'GAP' | 'NO_REASON';

export interface InventoryRow {
  readonly line: InventoryLineRDto;
  readonly counted: number | null;
  /** Écart compté − théorique ; null tant que l'article n'est pas compté. */
  readonly gap: number | null;
  readonly reason: InventoryReason | null;
}

/** Saisies de la session d'inventaire, dérivées de la session du serveur. */
@Injectable()
export class InventoryState {
  readonly session = signal<InventorySessionRDto | undefined>(undefined);
  readonly filter = signal<InventoryFilter>('ALL');

  readonly counts = linkedSignal<Readonly<Record<string, number | null>>>(() =>
    Object.fromEntries((this.session()?.lines ?? []).map((line) => [line.articleId, line.counted])),
  );
  readonly reasons = linkedSignal<Readonly<Record<string, InventoryReason | null>>>(() =>
    Object.fromEntries((this.session()?.lines ?? []).map((line) => [line.articleId, line.reason])),
  );

  readonly rows = computed<readonly InventoryRow[]>(() =>
    (this.session()?.lines ?? []).map((line) => {
      const counted = this.counts()[line.articleId] ?? null;
      return {
        line,
        counted,
        gap: counted === null ? null : counted - line.theoretical,
        reason: this.reasons()[line.articleId] ?? null,
      };
    }),
  );
  readonly counted = computed(() => this.rows().filter((row) => row.counted !== null));
  readonly gapRows = computed(() => this.rows().filter((row) => row.gap !== null && row.gap !== 0));
  readonly missingReasons = computed(() => this.gapRows().filter((row) => !row.reason));
  readonly totals = computed(() => ({
    units: this.gapRows().reduce((sum, row) => sum + (row.gap ?? 0), 0),
    value: this.gapRows().reduce((sum, row) => sum + (row.gap ?? 0) * row.line.unitCost, 0),
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

  setCount(articleId: string, raw: string): void {
    const value = raw.trim() === '' ? null : Math.max(0, Math.floor(Number(raw) || 0));
    this.counts.update((all) => ({ ...all, [articleId]: value }));
  }

  setReason(articleId: string, reason: string): void {
    this.reasons.update((all) => ({
      ...all,
      [articleId]: (reason || null) as InventoryReason | null,
    }));
  }

  toRequest(): InventoryCloseWDto {
    return {
      number: this.session()?.number ?? '',
      lines: this.counted().map((row) => ({
        articleId: row.line.articleId,
        counted: row.counted ?? 0,
        reason: row.reason,
      })),
    };
  }
}
