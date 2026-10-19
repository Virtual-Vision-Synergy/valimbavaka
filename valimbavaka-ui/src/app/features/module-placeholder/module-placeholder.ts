import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Page d'attente d'un module pas encore livré (ou « aucun accès ») : le titre vient de la donnée de route. */
@Component({
  selector: 'vvs-module-placeholder',
  templateUrl: './module-placeholder.html',
  styleUrl: './module-placeholder.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModulePlaceholder {
  private readonly data = inject(ActivatedRoute).snapshot.data;

  protected readonly heading = String(this.data['heading'] ?? '');
  protected readonly message = String(
    this.data['message'] ?? 'Ce module sera disponible prochainement.',
  );
}
