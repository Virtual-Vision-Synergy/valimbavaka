/** Actions déclenchables au clavier depuis le poste de caisse (VTE-03). */
export type ShortcutAction = 'search' | 'promo' | 'hold' | 'checkout' | 'increase' | 'decrease';

const FUNCTION_KEYS: Readonly<Record<string, ShortcutAction>> = {
  F2: 'search',
  F6: 'promo',
  F8: 'hold',
  F12: 'checkout',
};

const QUANTITY_KEYS: Readonly<Record<string, ShortcutAction>> = {
  '+': 'increase',
  '-': 'decrease',
  '−': 'decrease',
};

/** Actions qui valident quelque chose : une touche maintenue enfoncée ne doit pas les répéter. */
const ONCE_ONLY: ReadonlySet<ShortcutAction> = new Set<ShortcutAction>([
  'hold',
  'checkout',
  'promo',
]);

function target(event: KeyboardEvent): HTMLElement | null {
  return event.target instanceof HTMLElement ? event.target : null;
}

/** Vrai quand la frappe vise un champ de saisie : les touches + / − y restent du texte. */
function isTyping(event: KeyboardEvent): boolean {
  const element = target(event);
  return !!element && (element.isContentEditable || !!element.closest('input, textarea, select'));
}

/** Vrai quand la frappe vise une fenêtre modale (recherche globale, panneau) : le poste ne réagit pas. */
function isInDialog(event: KeyboardEvent): boolean {
  return !!target(event)?.closest('[role="dialog"], [aria-modal="true"]');
}

/** Vrai quand la frappe n'est pas pour la caisse : déjà traitée, combinaison Ctrl/Alt/⌘ ou fenêtre modale. */
function isForeign(event: KeyboardEvent): boolean {
  const modified = event.ctrlKey || event.altKey || event.metaKey;
  return event.defaultPrevented || modified || isInDialog(event);
}

/**
 * Action de caisse associée à une frappe, ou `undefined` si la touche doit rester au navigateur.
 * F2 / F6 / F8 / F12 marchent partout (même dans un champ) ; + et − seulement hors saisie. Les combinaisons avec
 * Ctrl, Alt ou ⌘ (zoom du navigateur Ctrl −, par exemple) sont laissées au navigateur.
 */
export function shortcutAction(event: KeyboardEvent): ShortcutAction | undefined {
  if (isForeign(event)) {
    return undefined;
  }
  const action =
    FUNCTION_KEYS[event.key] ?? (isTyping(event) ? undefined : QUANTITY_KEYS[event.key]);
  return action && event.repeat && ONCE_ONLY.has(action) ? undefined : action;
}
