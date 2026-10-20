import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

/** Page d'attente d'un module pas encore livré (ou « aucun accès ») : textes = clés de la donnée de route. */
@Component({
  selector: 'vvs-module-placeholder',
  imports: [TranslatePipe],
  templateUrl: './module-placeholder.html',
  styleUrl: './module-placeholder.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModulePlaceholder {
  private readonly data = inject(ActivatedRoute).snapshot.data;

  protected readonly headingKey = String(this.data['headingKey'] ?? '');
  protected readonly messageKey = String(this.data['messageKey'] ?? 'placeholder.comingSoon');
}
