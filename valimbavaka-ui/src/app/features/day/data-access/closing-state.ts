import { Injectable, computed, linkedSignal, signal } from '@angular/core';
import {
  ClosingLineRDto,
  DayClosingRDto,
  DayLockWDto,
  DiscrepancyReason,
  DispatchDestination,
  theoretical,
} from './day-models';

export interface ClosingRow {
  readonly key: string;
  readonly location: DispatchDestination;
  readonly line: ClosingLineRDto;
  readonly theoretical: number;
  readonly counted: number;
  readonly gap: number;
  readonly reason: DiscrepancyReason | null;
}

function keyOf(location: DispatchDestination, line: ClosingLineRDto): string {
  return `${location}:${line.articleId}`;
}

/** Saisies de la clôture (comptages, motifs, espèces) dérivées des données du serveur. */
@Injectable()
export class ClosingState {
  readonly data = signal<DayClosingRDto | undefined>(undefined);

  readonly counts = linkedSignal<Readonly<Record<string, number>>>(() =>
    this.collect((location, line) => line.counted),
  );
  readonly reasons = linkedSignal<Readonly<Record<string, DiscrepancyReason | null>>>(() =>
    this.collect((location, line) => line.reason),
  );
  readonly cash = linkedSignal<Readonly<Record<string, number>>>(() =>
    Object.fromEntries(
      (this.data()?.locations ?? []).map((item) => [item.location, item.cash.counted]),
    ),
  );
  readonly lateShootings = linkedSignal(() => this.data()?.checklist.lateShootings ?? 0);

  readonly rows = computed<readonly ClosingRow[]>(() =>
    (this.data()?.locations ?? []).flatMap(({ location, lines }) =>
      lines.map((line) => {
        const key = keyOf(location, line);
        const counted = this.counts()[key] ?? line.counted;
        const expected = theoretical(line);
        const reason = this.reasons()[key] ?? null;
        return {
          key,
          location,
          line,
          theoretical: expected,
          counted,
          gap: counted - expected,
          reason,
        };
      }),
    ),
  );
  readonly missingReasons = computed(
    () => this.rows().filter((row) => row.gap !== 0 && !row.reason).length,
  );

  setCount(key: string, raw: string): void {
    const value = Math.max(0, Math.floor(Number(raw) || 0));
    this.counts.update((all) => ({ ...all, [key]: value }));
  }

  setReason(key: string, reason: string): void {
    this.reasons.update((all) => ({ ...all, [key]: (reason || null) as DiscrepancyReason | null }));
  }

  setCash(location: DispatchDestination, raw: string): void {
    const value = Math.max(0, Math.round(Number(raw.replace(/\s/g, '')) || 0));
    this.cash.update((all) => ({ ...all, [location]: value }));
  }

  toRequest(date: string): DayLockWDto {
    return {
      date,
      counts: this.rows().map((row) => ({
        location: row.location,
        articleId: row.line.articleId,
        counted: row.counted,
        reason: row.reason,
      })),
      cash: Object.entries(this.cash()).map(([location, counted]) => ({
        location: location as DispatchDestination,
        counted,
      })),
    };
  }

  private collect<T>(
    pick: (location: DispatchDestination, line: ClosingLineRDto) => T,
  ): Record<string, T> {
    return Object.fromEntries(
      (this.data()?.locations ?? []).flatMap(({ location, lines }) =>
        lines.map((line) => [keyOf(location, line), pick(location, line)]),
      ),
    );
  }
}
