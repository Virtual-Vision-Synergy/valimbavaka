import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, tap, throwError } from 'rxjs';
import { ApiError } from '../../core/http/problem-detail';
import { ToastService } from '../components/toast/toast-service';
import { downloadFile, filenameOf } from './download-file';

/** Formats des routes jumelles générées par `@ExcelExport` côté serveur (SYS-01). */
export type ExportFormat = 'xlsx' | 'pdf';

const ROUTE_SUFFIX: Readonly<Record<ExportFormat, string>> = {
  xlsx: '/export',
  pdf: '/export/pdf',
};

/**
 * Export générique d'une liste : `<url de la liste>/export` (xlsx) ou `<url de la liste>/export/pdf`, avec les
 * critères de la liste affichée. Confirme la génération du fichier par un toast (ou signale l'échec) : l'écran
 * n'a qu'à appeler `download` et, s'il le souhaite, suivre l'état « en cours ». Le serveur décide des droits.
 */
@Injectable({ providedIn: 'root' })
export class ExportService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  /** Émet le nom du fichier téléchargé ; l'erreur est relayée après le toast d'échec. */
  download(
    listUrl: string,
    format: ExportFormat,
    params: HttpParams = new HttpParams(),
  ): Observable<string> {
    const fallbackName = `export.${format}`;
    const url = listUrl.replace(/\/+$/, '') + ROUTE_SUFFIX[format];
    return this.http.get(url, { params, observe: 'response', responseType: 'blob' }).pipe(
      map((response) => {
        downloadFile(response, fallbackName);
        return filenameOf(response, fallbackName);
      }),
      tap((filename) => this.toast.show('export.done', { filename })),
      catchError((error: unknown) => {
        this.toast.show(failureKey(error), {}, 'danger');
        return throwError(() => error);
      }),
    );
  }
}

function failureKey(error: unknown): string {
  const status =
    error instanceof ApiError || error instanceof HttpErrorResponse ? error.status : undefined;
  switch (status) {
    case 403:
      return 'export.forbidden';
    case 422:
      return 'export.tooLarge';
    default:
      return 'export.failed';
  }
}
