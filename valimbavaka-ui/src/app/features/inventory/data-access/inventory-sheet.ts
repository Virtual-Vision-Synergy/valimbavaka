import { InventoryLineRDto, InventorySessionRDto, InventorySheet } from './inventory-models';

const pad = (value: number): string => String(value).padStart(2, '0');

/** Instant ISO du serveur vers l'horodatage local `AAAA-MM-JJTHH:MM` affiché par la page. */
export function toLocalMinute(instant: string): string {
  const date = new Date(instant);
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}`
  );
}

/**
 * Jour local (`AAAA-MM-JJ`) où une session s'est terminée (INV-05) : jour de la clôture, sinon date du comptage
 * prévue à l'ouverture (session en cours ou annulée).
 */
export function closedOn(session: InventorySessionRDto): string {
  return session.closedAt ? toLocalMinute(session.closedAt).slice(0, 10) : session.countDate;
}

/** Session et lignes du serveur vers la feuille de comptage affichée (INV-02). */
export function toInventorySheet(
  session: InventorySessionRDto,
  lines: readonly InventoryLineRDto[],
): InventorySheet {
  return {
    id: session.id,
    number: session.number,
    locationLabel: session.locationLabel,
    openedAt: toLocalMinute(session.openedAt),
    openedBy: session.responsibleName,
    status: session.status,
    lines: lines.map((line) => ({
      lineId: line.id,
      articleId: line.articleId,
      reference: line.articleCode,
      barcode: line.barcode ?? '',
      name: line.articleName,
      theoretical: line.theoreticalQuantity,
      unitCost: line.unitCost,
      counted: line.countedQuantity,
      reason: line.reason,
    })),
  };
}

/** Quantité saisie : vide = non compté ; sinon positive, arrondie à 3 décimales comme le serveur. */
export function parseQuantity(raw: string): number | null {
  const value = Number(raw.trim().replace(',', '.'));
  if (raw.trim() === '' || !Number.isFinite(value)) {
    return null;
  }
  return Math.round(Math.max(0, value) * 1000) / 1000;
}

/** Écart compté − théorique arrondi à 3 décimales (évite 0.30000000000000004). */
export function gapOf(counted: number, theoretical: number): number {
  return Math.round((counted - theoretical) * 1000) / 1000;
}
