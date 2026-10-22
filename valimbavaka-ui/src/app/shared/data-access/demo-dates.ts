/** Date ISO locale (AAAA-MM-JJ) décalée de `offset` jours par rapport à aujourd'hui. */
export function isoDay(offset: number): string {
  const day = new Date();
  day.setDate(day.getDate() + offset);
  return [
    day.getFullYear(),
    String(day.getMonth() + 1).padStart(2, '0'),
    String(day.getDate()).padStart(2, '0'),
  ].join('-');
}
