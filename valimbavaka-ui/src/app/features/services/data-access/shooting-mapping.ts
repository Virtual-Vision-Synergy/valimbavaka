import { ShootingRDto, ShootingRowRDto, ShootingStatus } from './service-models';

const pad = (value: number): string => String(value).padStart(2, '0');

/** Instant ISO (UTC) → horodatage local `AAAA-MM-JJTHH:MM`, attendu par le pipe `when` et `datetime-local`. */
export function toLocalTimestamp(instant: string | Date): string {
  const date = new Date(instant);
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  return `${day}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Statut affiché : `RETURNED` une fois réintégrée, sinon `LATE` si le retour prévu est passé, sinon `OUT`. */
export function displayStatus(shooting: ShootingRDto): ShootingStatus {
  if (shooting.status === 'RETURNED') {
    return 'RETURNED';
  }
  return shooting.late ? 'LATE' : 'OUT';
}

/** Convertit la sortie du serveur en ligne de `ShootingList` (composant inchangé). */
export function toShootingRow(shooting: ShootingRDto): ShootingRowRDto {
  return {
    number: shooting.number,
    items: shooting.items.map((item) => ({ name: item.articleName, quantity: item.quantity })),
    takenBy: shooting.takenBy,
    expectedReturn: toLocalTimestamp(shooting.expectedReturnAt),
    status: displayStatus(shooting),
  };
}
