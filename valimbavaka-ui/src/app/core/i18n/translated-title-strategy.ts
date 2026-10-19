import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

const SUFFIX = ' · Valimbavaka';

/** Le `title` d'une route est une clé de traduction ; un texte qui n'en est pas une est conservé tel quel. */
@Injectable({ providedIn: 'root' })
export class TranslatedTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly translate = inject(TranslateService);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const key = this.buildTitle(snapshot);
    if (key === undefined) {
      return;
    }
    const translated = this.translate.instant(key);
    this.title.setTitle(translated === key ? key : translated + SUFFIX);
  }
}
