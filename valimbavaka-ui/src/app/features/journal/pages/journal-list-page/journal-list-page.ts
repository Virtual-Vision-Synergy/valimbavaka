import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Lock } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { Icon } from '../../../../shared/components/icon/icon';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { Tone } from '../../../../shared/models/tone';
import { JournalAction, JournalEntryRDto } from '../../data-access/journal-models';
import { JournalService } from '../../data-access/journal-service';

const PAGE_SIZE = 20;

/** Jour UTC courant au format yyyy-MM-dd, comme le contrat de l'API. */
const todayUtc = (): string => new Date().toISOString().slice(0, 10);

const ACTION_TONES: Readonly<Record<JournalAction, Tone>> = {
  CREATE: 'success',
  UPDATE: 'info',
  DELETE: 'danger',
  CANCEL: 'warning',
  REVERSE: 'danger',
};

/** Consultation du journal des opérations (JNL-01) : par défaut les opérations du jour UTC en ordre chronologique (JNL-03). */
@Component({
  selector: 'vvs-journal-list-page',
  imports: [DatePipe, TranslatePipe, Icon, PageHeader, StatusBadge],
  templateUrl: './journal-list-page.html',
  styleUrl: './journal-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalListPage {
  private readonly journalService = inject(JournalService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly icons = { Lock };
  protected readonly tones = ACTION_TONES;
  protected readonly entries = signal<JournalEntryRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalPages = signal(0);
  protected readonly totalCount = signal(0);
  protected readonly search = signal('');
  /** Jour affiché (yyyy-MM-dd, UTC) ; vide = tout le journal, du plus récent au plus ancien. */
  protected readonly day = signal(todayUtc());
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.loadEntries();
  }

  protected onSearch(term: string): void {
    this.search.set(term.trim());
    this.page.set(1);
    this.loadEntries();
  }

  protected onDayChange(day: string): void {
    this.day.set(day);
    this.page.set(1);
    this.loadEntries();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadEntries();
  }

  private loadEntries(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.journalService
      .list({ search: this.search(), page: this.page(), size: PAGE_SIZE }, this.day())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (result) => {
          this.entries.set(result.items);
          this.totalPages.set(result.totalPages);
          this.totalCount.set(result.totalCount);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }
}
