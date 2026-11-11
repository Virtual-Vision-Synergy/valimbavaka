/** Mois `AAAA-MM` décalé de `offset` mois par rapport à `from`. */
export function monthKey(from: Date, offset: number): string {
  const day = new Date(from.getFullYear(), from.getMonth() + offset, 1);
  return `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}`;
}

/**
 * Jours restants du mois `AAAA-MM`, aujourd'hui compris : 0 pour un mois passé, tout le mois pour un mois à venir.
 */
export function daysLeftInMonth(month: string, today: Date): number {
  const [year, number] = month.split('-').map(Number);
  const daysInMonth = new Date(year, number, 0).getDate();
  const current = monthKey(today, 0);
  if (month < current) {
    return 0;
  }
  return month === current ? daysInMonth - today.getDate() + 1 : daysInMonth;
}
