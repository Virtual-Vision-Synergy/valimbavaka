import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { Upload, X } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';

const MAX_BYTES = 2 * 1024 * 1024;
const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp'];

/** Dépôt de photo (glisser-déposer ou parcourir) avec aperçu local ; envoi au serveur à venir. */
@Component({
  selector: 'vvs-photo-drop',
  imports: [TranslatePipe, Icon],
  templateUrl: './photo-drop.html',
  styleUrl: './photo-drop.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PhotoDrop {
  protected readonly icons = { Upload, X };
  protected readonly preview = signal<string | null>(null);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly dragging = signal(false);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.revoke());
  }

  protected pick(files: FileList | null): void {
    const file = files?.item(0);
    if (!file) {
      return;
    }
    if (!ACCEPTED.includes(file.type)) {
      this.errorKey.set('articles.photo.typeError');
      return;
    }
    if (file.size > MAX_BYTES) {
      this.errorKey.set('articles.photo.sizeError');
      return;
    }
    this.errorKey.set(null);
    this.revoke();
    this.preview.set(URL.createObjectURL(file));
  }

  protected drop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.pick(event.dataTransfer?.files ?? null);
  }

  protected dragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected remove(): void {
    this.revoke();
    this.preview.set(null);
  }

  private revoke(): void {
    const url = this.preview();
    if (url) {
      URL.revokeObjectURL(url);
    }
  }
}
