/** Corps d'erreur RFC 9457 renvoyé par le GlobalExceptionHandler du backend (convention §7.4). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  code?: ProblemCode;
  errors?: FieldErrorDetail[];
}

export type ProblemCode =
  | 'MALFORMED_REQUEST'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'VALIDATION_FAILED'
  | 'RATE_LIMITED'
  | 'INTERNAL_ERROR';

export interface FieldErrorDetail {
  field: string;
  message: string;
}

const DEFAULT_ERROR_MESSAGE = 'Une erreur inattendue est survenue';
const NETWORK_ERROR_MESSAGE = 'Le serveur est injoignable. Vérifiez votre connexion.';

/** Erreur typée émise par l'errorInterceptor à la place d'une HttpErrorResponse brute. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: ProblemCode | undefined,
    message: string,
    readonly fieldErrors: readonly FieldErrorDetail[] = [],
  ) {
    super(message);
    this.name = 'ApiError';
  }

  static fromProblem(status: number, body: unknown): ApiError {
    if (status === 0) {
      return new ApiError(0, undefined, NETWORK_ERROR_MESSAGE);
    }
    if (!isProblemDetail(body)) {
      return new ApiError(status, undefined, DEFAULT_ERROR_MESSAGE);
    }
    return new ApiError(status, body.code, body.detail ?? DEFAULT_ERROR_MESSAGE, body.errors ?? []);
  }
}

/** Message lisible pour l'utilisateur, quelle que soit l'erreur reçue. */
export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : DEFAULT_ERROR_MESSAGE;
}

function isProblemDetail(body: unknown): body is ProblemDetail {
  return typeof body === 'object' && body !== null && 'status' in body;
}
