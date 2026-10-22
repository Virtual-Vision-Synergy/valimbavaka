import { Pipe, PipeTransform, inject } from '@angular/core';
import { LanguageService } from '../../../core/i18n/language-service';
import { isoDay } from '../../../shared/data-access/demo-dates';

/** « Auj. 10:15 », « Hier 15:20 » ou « 19 oct. 14:10 » d'un horodatage local `AAAA-MM-JJTHH:MM`. */
@Pipe({ name: 'when', pure: false })
export class WhenPipe implements PipeTransform {
  private readonly language = inject(LanguageService);

  transform(timestamp: string): string {
    const [date, time] = timestamp.split('T');
    const locale = this.language.current();
    const relative = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
    if (date === isoDay(0) || date === isoDay(-1)) {
      const label = relative.format(date === isoDay(0) ? 0 : -1, 'day');
      return `${label.charAt(0).toUpperCase()}${label.slice(1)} ${time}`;
    }
    const short = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' });
    return `${short.format(new Date(`${date}T12:00:00`))} ${time}`;
  }
}
