import { Pipe, PipeTransform, inject } from '@angular/core';
import { LanguageService } from '../../core/i18n/language-service';

/** Date courte localisée (« 21 oct. ») d'une date ISO `AAAA-MM-JJ`. */
@Pipe({ name: 'shortDate', pure: false })
export class ShortDatePipe implements PipeTransform {
  private readonly language = inject(LanguageService);

  transform(isoDate: string): string {
    return new Intl.DateTimeFormat(this.language.current(), {
      day: '2-digit',
      month: 'short',
    }).format(new Date(`${isoDate}T12:00:00`));
  }
}
