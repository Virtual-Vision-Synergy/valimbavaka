import { shortcutAction } from './pos-shortcuts';

const press = (key: string, init: KeyboardEventInit = {}, on?: HTMLElement): KeyboardEvent => {
  const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...init });
  on?.dispatchEvent(event);
  return event;
};

/** Raccourcis de caisse (VTE-03) : quelle touche déclenche quelle action. */
describe('shortcutAction', () => {
  afterEach(() => document.body.replaceChildren());

  it.each([
    ['F2', 'search'],
    ['F6', 'promo'],
    ['F8', 'hold'],
    ['F12', 'checkout'],
    ['+', 'increase'],
    ['-', 'decrease'],
    ['−', 'decrease'],
  ])('associe la touche %s à l’action %s', (key, action) => {
    expect(shortcutAction(press(key))).toBe(action);
  });

  it('ignore les touches non définies', () => {
    expect(shortcutAction(press('F5'))).toBeUndefined();
    expect(shortcutAction(press('a'))).toBeUndefined();
  });

  it('laisse au navigateur les combinaisons Ctrl, Alt et ⌘ (zoom Ctrl −)', () => {
    expect(shortcutAction(press('-', { ctrlKey: true }))).toBeUndefined();
    expect(shortcutAction(press('+', { metaKey: true }))).toBeUndefined();
    expect(shortcutAction(press('F12', { altKey: true }))).toBeUndefined();
  });

  it('garde + et − comme du texte dans un champ de saisie, mais F12 reste actif', () => {
    const input = document.body.appendChild(document.createElement('input'));

    expect(shortcutAction(press('+', {}, input))).toBeUndefined();
    expect(shortcutAction(press('-', {}, input))).toBeUndefined();
    expect(shortcutAction(press('F12', {}, input))).toBe('checkout');
  });

  it('ne répète pas encaisser ni mettre en attente quand la touche reste enfoncée', () => {
    expect(shortcutAction(press('F12', { repeat: true }))).toBeUndefined();
    expect(shortcutAction(press('F8', { repeat: true }))).toBeUndefined();
    expect(shortcutAction(press('+', { repeat: true }))).toBe('increase');
  });

  it('ne réagit pas dans une fenêtre modale (recherche globale)', () => {
    const dialog = document.body.appendChild(document.createElement('div'));
    dialog.setAttribute('role', 'dialog');
    const button = dialog.appendChild(document.createElement('button'));

    expect(shortcutAction(press('F12', {}, button))).toBeUndefined();
  });

  it('respecte une frappe déjà traitée par un autre composant', () => {
    const event = press('F2', { cancelable: true });
    event.preventDefault();

    expect(shortcutAction(event)).toBeUndefined();
  });
});
