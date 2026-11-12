import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ExportFormat, ExportService } from '../../../shared/data-access/export-service';
import { ReportQuery, ReportRDto } from './report-models';

/**
 * Rapports (RPT-01 à RPT-03) : `GET /api/v1/reports` (permission `reports.read`, administrateur). Chiffre d'affaires,
 * ventes validées et panier moyen du jour, du mois ou de l'année, filtrés par canal et par gamme, comparés à la
 * période précédente (valeur et variation en %), avec la courbe du CA (un point par jour, ou par mois pour une année).
 * `export` (RPT-06) télécharge le même rapport en xlsx (`/export`) ou en PDF (`/export/pdf`) avec les mêmes critères.
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly exportService = inject(ExportService);
  private readonly url = '/api/v1/reports';

  get(query: ReportQuery): Observable<ReportRDto> {
    return this.http.get<ReportRDto>(this.url, { params: toParams(query) });
  }

  /** Émet le nom du fichier ; le service d'export confirme la génération (ou l'échec) par une notification. */
  export(query: ReportQuery, format: ExportFormat): Observable<string> {
    return this.exportService.download(this.url, format, toParams(query));
  }
}

function toParams(query: ReportQuery): HttpParams {
  let params = new HttpParams();
  for (const key of ['granularity', 'date', 'salesChannelId', 'rangeId'] as const) {
    const value = query[key];
    if (value) {
      params = params.set(key, value);
    }
  }
  return params;
}
