import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';
import { AuthStore } from './auth-store';

/** Affiche le bloc si l'utilisateur courant possède la permission : `*vvsHasPermission="'users.create'"`. */
@Directive({ selector: '[vvsHasPermission]' })
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly authStore = inject(AuthStore);

  readonly vvsHasPermission = input.required<string>();

  constructor() {
    effect(() => {
      this.viewContainerRef.clear();
      if (this.authStore.hasPermission(this.vvsHasPermission())) {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
      }
    });
  }
}
