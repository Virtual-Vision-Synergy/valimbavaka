import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ApiError } from './problem-detail';

/** Convertit toute erreur HTTP en ApiError typée (ProblemDetail du backend). */
export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: unknown) =>
      throwError(() =>
        error instanceof HttpErrorResponse
          ? ApiError.fromProblem(error.status, error.error)
          : error,
      ),
    ),
  );
