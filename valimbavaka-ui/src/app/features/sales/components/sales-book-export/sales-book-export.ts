import { HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { Download } from 'lucide';
import { finalize } from 'rxjs';
import { Icon } from '../../../../shared/components/icon/icon';
import { ExportFormat, ExportService } from '../../../../shared/data-access/export-service';
import { localIsoDate } from '../../data-access/sales-mapping';

/** Route du livre des ventes : `<route>/export` (xlsx) et `<route>/export/pdf` (FAC-06). */
export const SALES_BOOK_URL = '/api/v1/invoices/sales-book';

/** Plus longue période acceptée par le serveur, en jours (bornes comprises). */
export const SALES_BOOK_MAX_DAYS = 366;

const DAY_MS = 24 * 60 * 60 * 1000;

/** Premier jour du mois de `date`, au format `yyyy-MM-dd` (jour local). */
function firstOfMonth(date: Date): string {
  return localIsoDate(new Date(date.getFullYear(), date.getMonth(), 1));
}

/**
 * Export du livre des ventes (FAC-06, administrateur) : une période (mois en cours par défaut), puis un fichier XLSX
 * ou PDF avec les factures et leurs avoirs (en négatif). Une période à l'envers ou trop longue n'est pas envoyée : le
 * serveur la refuserait (422). La confirmation de génération est le toast d'`ExportService`.
 */
@Component({
  selector: 'vvs-sales-book-export',
  imports: [TranslatePipe, Icon],
  templateUrl: './sales-book-export.html',
  styleUrl: './sales-book-export.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalesBookExport {
  private readonly exportService = inject(ExportService);

  protected readonly icons = { Download };
  protected readonly from = signal(firstOfMonth(new Date()));
  protected readonly to = signal(localIsoDate(new Date()));
  protected readonly exporting = signal(false);
  protected readonly invalidPeriod = computed(() => {
    const from = this.from();
    const to = this.to();
    if (!from || !to) {
      return true;
    }
    const days = (Date.parse(to) - Date.parse(from)) / DAY_MS;
    return days < 0 || days >= SALES_BOOK_MAX_DAYS;
  });

  protected onFromChange(event: Event): void {
    this.from.set((event.target as HTMLInputElement).value);
  }

  protected onToChange(event: Event): void {
    this.to.set((event.target as HTMLInputElement).value);
  }

  protected export(format: ExportFormat): void {
    if (this.invalidPeriod() || this.exporting()) {
      return;
    }
    this.exporting.set(true);
    const params = new HttpParams().set('from', this.from()).set('to', this.to());
    this.exportService
      .download(SALES_BOOK_URL, format, params)
      .pipe(finalize(() => this.exporting.set(false)))
      .subscribe({ error: () => undefined }); // toast d'échec déjà affiché par le service
  }
}
