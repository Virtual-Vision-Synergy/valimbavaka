import { Directive, TemplateRef, ViewContainerRef, inject, input, effect } from '@angular/core';

@Directive({ selector: '[appHasPermission]' })
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainerRef = inject(ViewContainerRef);

  readonly appHasPermission = input.required<string>();

  constructor() {
    effect(() => {
      // TODO (feature auth) : vérifier la permission réelle de l'utilisateur courant
      const hasPermission = false;
      this.viewContainerRef.clear();
      if (hasPermission) {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
      }
    });
  }
}
