import { DEMO_ARTICLES, StockLocation } from '../../../shared/data-access/demo-catalog';
import { isoDay } from '../../../shared/data-access/demo-dates';
import {
  TransferDetailRDto,
  TransferLineRDto,
  TransferOrigin,
  TransferRDto,
  TransferStatus,
} from './transfer-models';

/** [n°, de, vers, origine, lignes, unités, jours avant aujourd'hui, heure, statut]. */
type TransferRow = readonly [
  string,
  StockLocation,
  StockLocation,
  TransferOrigin,
  number,
  number,
  number,
  string,
  TransferStatus,
];

const ROWS: readonly TransferRow[] = [
  ['TRF-0095', 'SHOP', 'SALON', 'MANUAL', 2, 6, 0, '10:15', 'IN_TRANSIT'],
  ['RET-0015', 'DELIVERY', 'WAREHOUSE', 'RETURN', 3, 5, 0, '09:40', 'RETURN'],
  ['TRF-0094', 'WAREHOUSE', 'SALON', 'MORNING', 9, 32, 0, '07:42', 'RECEIVED'],
  ['TRF-0093', 'WAREHOUSE', 'DELIVERY', 'MORNING', 14, 56, 0, '07:42', 'RECEIVED'],
  ['TRF-0092', 'WAREHOUSE', 'SHOP', 'MORNING', 38, 312, 0, '07:42', 'RECEIVED'],
  ['TRF-0091', 'WAREHOUSE', 'SHOP', 'MANUAL', 4, 24, 1, '15:20', 'DISCREPANCY'],
  ['RET-0014', 'SHOP', 'WAREHOUSE', 'RETURN', 2, 3, 1, '19:05', 'RETURN'],
  ['TRF-0090', 'WAREHOUSE', 'DELIVERY', 'MORNING', 12, 48, 1, '07:35', 'RECEIVED'],
  ['TRF-0089', 'WAREHOUSE', 'SHOP', 'MORNING', 36, 288, 1, '07:35', 'RECEIVED'],
  ['TRF-0088', 'WAREHOUSE', 'SALON', 'MORNING', 8, 28, 1, '07:35', 'RECEIVED'],
  ['TRF-0087', 'SHOP', 'DELIVERY', 'MANUAL', 3, 12, 2, '14:10', 'RECEIVED'],
  ['TRF-0086', 'WAREHOUSE', 'SHOP', 'MORNING', 35, 270, 2, '07:40', 'RECEIVED'],
  ['RET-0013', 'DELIVERY', 'WAREHOUSE', 'RETURN', 4, 7, 2, '18:30', 'RETURN'],
  ['TRF-0085', 'WAREHOUSE', 'DELIVERY', 'MORNING', 11, 44, 2, '07:40', 'RECEIVED'],
];

const SHOWN_LINES = 6;

function toTransfer([
  number,
  from,
  to,
  origin,
  lineCount,
  unitCount,
  ago,
  time,
  status,
]: TransferRow): TransferRDto {
  return {
    id: number.toLowerCase(),
    number,
    from,
    to,
    origin,
    lineCount,
    unitCount,
    when: `${isoDay(-ago)}T${time}`,
    status,
  };
}

export const TRANSFERS_FIXTURE: readonly TransferRDto[] = ROWS.map(toTransfer);

/** Répartit les unités du bon sur ses premières lignes (les plus grosses d'abord). */
function linesOf(transfer: TransferRDto, offset: number): readonly TransferLineRDto[] {
  const finished = DEMO_ARTICLES.filter((article) => article.type === 'FINISHED' && !article.box);
  const shown = Math.min(SHOWN_LINES, transfer.lineCount);
  const weights = Array.from({ length: shown }, (_, rank) => shown - rank);
  const weightSum = weights.reduce((sum, weight) => sum + weight, 0) + (transfer.lineCount - shown);
  return weights.map((weight, rank) => {
    const article = finished[(offset + rank) % finished.length];
    const quantity = Math.max(1, Math.round((transfer.unitCount * weight) / weightSum));
    const missing = transfer.status === 'DISCREPANCY' && rank === 0 ? 2 : 0;
    return {
      reference: article.reference,
      name: article.name,
      quantity,
      received: transfer.status === 'IN_TRANSIT' ? 0 : quantity - missing,
    };
  });
}

export function transferDetailFixture(id: string): TransferDetailRDto | null {
  const index = TRANSFERS_FIXTURE.findIndex((transfer) => transfer.id === id);
  if (index < 0) {
    return null;
  }
  const transfer = TRANSFERS_FIXTURE[index];
  return {
    ...transfer,
    operator: 'Hanta R.',
    lines: linesOf(transfer, index),
    hiddenLines: Math.max(0, transfer.lineCount - SHOWN_LINES),
  };
}
