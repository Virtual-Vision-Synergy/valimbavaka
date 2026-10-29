import { HttpResponse } from '@angular/common/http';

const FILENAME = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i;

/** Nom du fichier annoncé par `Content-Disposition`, ou `fallback` s'il est absent. */
export function filenameOf(response: HttpResponse<Blob>, fallback: string): string {
  const match = FILENAME.exec(response.headers.get('Content-Disposition') ?? '');
  return match ? decodeURIComponent(match[1]) : fallback;
}

/** Fait télécharger au navigateur le corps d'une réponse `blob`. */
export function downloadFile(response: HttpResponse<Blob>, fallbackName: string): void {
  if (!response.body) {
    return;
  }
  const url = URL.createObjectURL(response.body);
  const link = document.createElement('a');
  link.href = url;
  link.download = filenameOf(response, fallbackName);
  link.click();
  URL.revokeObjectURL(url);
}
