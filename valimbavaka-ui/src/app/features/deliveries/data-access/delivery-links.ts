/** Lien d'appel : le numéro sans espaces, composé directement par le téléphone. */
export function telHref(phone: string): string {
  return `tel:${phone.replaceAll(' ', '')}`;
}

/** Itinéraire vers l'adresse dans l'application de cartes du téléphone. */
export function mapsHref(address: string): string {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${address}, Antananarivo`)}`;
}
