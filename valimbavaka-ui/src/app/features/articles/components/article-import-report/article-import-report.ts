import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import {
  ArticleImportRDto,
  ArticleImportRowRDto,
} from '../../../article/data-access/article-models';

/**
 * Compte rendu d'un import CSV (ART-07) : compteurs, lignes rejetées avec leur motif, et le choix d'écraser les
 * articles en conflit. Le conflit n'écrase rien tant que l'administrateur ne le demande pas.
 */
@Component({
  selector: 'vvs-article-import-report',
  imports: [TranslatePipe],
  templateUrl: './article-import-report.html',
  styleUrl: './article-import-report.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleImportReport {
  readonly report = input.required<ArticleImportRDto>();
  readonly busy = input(false);
  readonly overwrite = output<void>();
  readonly dismiss = output<void>();

  /** Lignes à traiter à la main : conflits et lignes invalides. */
  protected readonly rejected = computed<readonly ArticleImportRowRDto[]>(() =>
    this.report().rows.filter((row) => row.status === 'CONFLICT' || row.status === 'INVALID'),
  );
}
