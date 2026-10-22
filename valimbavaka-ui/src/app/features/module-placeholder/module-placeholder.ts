import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../shared/components/list-state/list-state';
import { PageHeader } from '../../shared/components/page-header/page-header';

/** Page d'attente d'un module pas encore livré (ou « aucun accès ») : textes = clés de la donnée de route. */
@Component({
  selector: 'vvs-module-placeholder',
  imports: [TranslatePipe, PageHeader, ListState],
  templateUrl: './module-placeholder.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModulePlaceholder {
  private readonly data = inject(ActivatedRoute).snapshot.data;

  protected readonly headingKey = String(this.data['headingKey'] ?? '');
  protected readonly messageKey = String(this.data['messageKey'] ?? 'placeholder.comingSoon');
}
