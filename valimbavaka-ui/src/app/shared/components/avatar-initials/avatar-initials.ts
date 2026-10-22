import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Pastille d'initiales (personne, article) ; décorative : le nom complet est toujours écrit à côté. */
@Component({
  selector: 'vvs-avatar-initials',
  template: `<span
    class="avatar"
    [attr.data-tone]="tone()"
    [style.--size.px]="size()"
    aria-hidden="true"
  >
    {{ initials() }}
  </span>`,
  styleUrl: './avatar-initials.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvatarInitials {
  readonly name = input.required<string>();
  readonly size = input(40);
  readonly tone = input<'primary' | 'secondary' | 'muted'>('primary');

  protected readonly initials = computed(() =>
    this.name()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word.charAt(0).toUpperCase())
      .join(''),
  );
}
