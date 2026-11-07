import { OutboxEntry } from './outbox-models';

/**
 * Règle d'ordre de synchronisation en cas de blocage (SYS-10, décidée par le PO le 07/11/2026 :
 * « ordre strict par type »).
 *
 * - ORDERED : opérations métier rejouables (journal, ventes, factures, avoirs, stock, transferts, production,
 *   inventaire, livraisons…). Elles partent dans l'ordre strict de saisie et attendent derrière une saisie
 *   bloquée par le réseau de leur groupe, même quand ses relances sont épuisées (FAILED réseau).
 * - INDEPENDENT : fiches et référentiels (fournisseurs, articles, utilisateurs, promotions, catalogues…). Elles
 *   peuvent passer devant une saisie bloquée par le réseau ; seules les saisies d'une même fiche restent dans
 *   l'ordre entre elles.
 * - Adresse non classée : comportement d'avant SYS-10 (file unique), elle attend derrière toute saisie en cours
 *   de relance et retient toutes les suivantes. Un ticket qui ajoute un endpoint d'écriture le classe ici.
 */
export type SyncOrderKind = 'ORDERED' | 'INDEPENDENT';

export interface SyncOrderRule {
  /** Préfixe d'adresse, sans barre finale : `/api/v1/sales` couvre `/api/v1/sales` et `/api/v1/sales/…`. */
  readonly prefix: string;
  readonly kind: SyncOrderKind;
}

/** Couloir d'une saisie : deux saisies d'un même couloir partent toujours dans l'ordre de saisie. */
export interface SyncLane {
  readonly key: string;
  readonly kind: SyncOrderKind | 'UNCLASSIFIED';
}

/** Groupe unique des opérations : une vente, sa facture et le mouvement de stock restent dans l'ordre. */
export const OPERATIONS_GROUP = 'operations';
const UNCLASSIFIED_KEY = '*';

/** Table de classement ; pour un nouveau type d'écriture, ajouter une ligne (voir docs/ticket/SYS-10/guide.md). */
export const SYNC_ORDER_RULES: readonly SyncOrderRule[] = [
  { prefix: '/api/v1/journal', kind: 'ORDERED' },
  { prefix: '/api/v1/sales', kind: 'ORDERED' },
  { prefix: '/api/v1/invoices', kind: 'ORDERED' },
  { prefix: '/api/v1/credit-notes', kind: 'ORDERED' },
  { prefix: '/api/v1/stocks', kind: 'ORDERED' },
  { prefix: '/api/v1/transfers', kind: 'ORDERED' },
  { prefix: '/api/v1/production', kind: 'ORDERED' },
  { prefix: '/api/v1/inventories', kind: 'ORDERED' },
  { prefix: '/api/v1/deliveries', kind: 'ORDERED' },
  { prefix: '/api/v1/rounds', kind: 'ORDERED' },
  { prefix: '/api/v1/days', kind: 'ORDERED' },
  { prefix: '/api/v1/shootings', kind: 'ORDERED' },
  { prefix: '/api/v1/suppliers', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/articles', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/users', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/promotions', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/catalogs', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/services', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/product-ranges', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/sales-channels', kind: 'INDEPENDENT' },
  { prefix: '/api/v1/stock-locations', kind: 'INDEPENDENT' },
];

/** Couloir de la saisie : groupe des opérations, fiche visée, ou file unique si l'adresse n'est pas classée. */
export function syncLane(
  url: string,
  rules: readonly SyncOrderRule[] = SYNC_ORDER_RULES,
): SyncLane {
  const path = url.split(/[?#]/)[0];
  const rule = rules.find((candidate) => matches(path, candidate.prefix));
  if (!rule) {
    return { key: UNCLASSIFIED_KEY, kind: 'UNCLASSIFIED' };
  }
  if (rule.kind === 'ORDERED') {
    return { key: `group:${OPERATIONS_GROUP}`, kind: 'ORDERED' };
  }
  const recordId = path.slice(rule.prefix.length).split('/').filter(Boolean)[0];
  return { key: `record:${rule.prefix}/${recordId ?? ''}`, kind: 'INDEPENDENT' };
}

function matches(path: string, prefix: string): boolean {
  return path === prefix || path.startsWith(`${prefix}/`);
}

/**
 * Prochaine saisie à envoyer, dans l'ordre de la file. Une saisie est retenue si son couloir est bloqué par une
 * saisie plus ancienne : en attente de relance (`isDue` faux), ou FAILED réseau pour le groupe des opérations.
 * Une saisie refusée par le serveur (FAILED métier) ne retient rien (ADR 0001).
 */
export function nextSendableEntry(
  entries: readonly OutboxEntry[],
  isDue: (entry: OutboxEntry) => boolean,
): OutboxEntry | undefined {
  const waiting = new Set<string>();
  const stuck = new Set<string>();
  for (const entry of entries) {
    const lane = syncLane(entry.url);
    if (entry.status === 'FAILED') {
      if (entry.networkFailure && lane.kind === 'ORDERED') {
        stuck.add(lane.key);
      }
    } else if (!stuck.has(lane.key)) {
      if (!isWaiting(lane, waiting) && isDue(entry)) {
        return entry;
      }
      waiting.add(lane.key);
    }
  }
  return undefined;
}

function isWaiting(lane: SyncLane, waiting: ReadonlySet<string>): boolean {
  return (
    waiting.has(UNCLASSIFIED_KEY) ||
    waiting.has(lane.key) ||
    (lane.kind === 'UNCLASSIFIED' && waiting.size > 0)
  );
}
