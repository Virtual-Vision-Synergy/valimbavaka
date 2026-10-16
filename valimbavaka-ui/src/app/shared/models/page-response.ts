/** Contrat de pagination VVS, miroir de shared/dto/PageResponse côté backend (page en base 1). */
export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

/** Paramètres de liste communs, miroir de shared/dto/PageQuery. */
export interface PageQuery {
  page: number;
  size: number;
  search?: string;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}
