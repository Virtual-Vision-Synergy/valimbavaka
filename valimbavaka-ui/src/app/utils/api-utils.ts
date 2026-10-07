import { HttpErrorResponse } from '@angular/common/http';
import { ApiResponse } from '../models/api-response';

const DEFAULT_ERROR_MESSAGE = 'Une erreur inattendue est survenue';

/** Extrait `data` d'une ApiResponse ; un succès sans donnée est une anomalie de contrat. */
export function unwrapData<T>(response: ApiResponse<T>): T {
  if (response.data === null) {
    throw new Error(response.message || DEFAULT_ERROR_MESSAGE);
  }
  return response.data;
}

/** Message lisible depuis une erreur HTTP renvoyée par le GlobalExceptionHandler. */
export function extractErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiResponse<unknown>> | null;
    return body?.message ?? DEFAULT_ERROR_MESSAGE;
  }
  return error instanceof Error ? error.message : DEFAULT_ERROR_MESSAGE;
}
