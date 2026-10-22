import { Pipe, PipeTransform } from '@angular/core';

const FULL = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });
const COMPACT = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });

export type AriaryFormat = 'full' | 'plain' | 'compact' | 'kilo';

/** Formate un montant en ariary (espaces de milliers, sans décimale) : utilisable hors template. */
export function formatAriary(amount: number, format: AriaryFormat = 'full'): string {
  if (format === 'compact') {
    return `${COMPACT.format(amount / 1_000_000)} M`;
  }
  if (format === 'plain') {
    return FULL.format(amount);
  }
  if (format === 'kilo') {
    return `${FULL.format(Math.round(amount / 1000))} k`;
  }
  return `${FULL.format(amount)} Ar`;
}

/** `full` → « 1 284 000 Ar », `plain` → « 1 284 000 », `compact` → « 1,28 M », `kilo` → « 668 k ». */
@Pipe({ name: 'ariary' })
export class AriaryPipe implements PipeTransform {
  transform(amount: number, format: AriaryFormat = 'full'): string {
    return formatAriary(amount, format);
  }
}
