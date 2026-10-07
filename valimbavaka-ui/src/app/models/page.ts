/** Miroir de PageResponse côté backend (common/dto/PageResponse). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
