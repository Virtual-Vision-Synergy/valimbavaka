import { Pipe, PipeTransform, inject } from '@angular/core';
import { LanguageService } from '../../core/i18n/language-service';

/**
 * Ancienneté lisible dans la langue courante : « il y a 2 min », « il y a 14 h », « hier ».
 * Impur (léger) pour suivre un changement de langue sans recharger la page.
 */
@Pipe({ name: 'relativeTime', pure: false })
export class RelativeTimePipe implements PipeTransform {
  private readonly language = inject(LanguageService);

  transform(minutesAgo: number): string {
    const format = new Intl.RelativeTimeFormat(this.language.current(), {
      numeric: 'auto',
      style: 'short',
    });
    if (minutesAgo < 60) {
      return format.format(-minutesAgo, 'minute');
    }
    if (minutesAgo < 24 * 60) {
      return format.format(-Math.round(minutesAgo / 60), 'hour');
    }
    return format.format(-Math.round(minutesAgo / (24 * 60)), 'day');
  }
}
