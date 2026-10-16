/** Référentiels métier (SYS-05) : `code` stable pour le code, `label` pour l'affichage. */
export interface SalesChannelRDto {
  id: string;
  code: string;
  label: string;
}

export interface ProductRangeRDto {
  id: string;
  code: string;
  label: string;
}

export interface StockLocationRDto {
  id: string;
  code: string;
  label: string;
}
