import { Pipe, PipeTransform, inject } from '@angular/core';
import { LanguageService } from '../../../core/i18n/language-service';
import { isoDay } from '../../../shared/data-access/demo-dates';

/** Instant du serveur, en UTC ou avec un décalage (`2026-11-08T07:42:10.5Z`, `…+03:00`). */
const INSTANT = /(Z|[+-]\d{2}:\d{2})$/i;

/** Horodatage local `AAAA-MM-JJTHH:MM` d'un instant ISO du serveur (fuseau du navigateur). */
export function toLocalTimestamp(instant: string): string {
  const date = new Date(instant);
  const pad = (value: number): string => String(value).padStart(2, '0');
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  return `${day}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/**
 * « Auj. 10:15 », « Hier 15:20 » ou « 19 oct. 14:10 » d'un horodatage local `AAAA-MM-JJTHH:MM` ou d'un instant
 * ISO du serveur, ramené au fuseau du navigateur.
 */
@Pipe({ name: 'when', pure: false })
export class WhenPipe implements PipeTransform {
  private readonly language = inject(LanguageService);

  transform(timestamp: string): string {
    const local = INSTANT.test(timestamp) ? toLocalTimestamp(timestamp) : timestamp;
    const [date, time] = local.split('T');
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
