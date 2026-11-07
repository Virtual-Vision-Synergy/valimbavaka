import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { AuthStore } from '../../../core/auth/auth-store';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { ReferentialService } from '../../referential/data-access/referential-service';
import { toPosContext } from './pos-mapping';
import { PosContextRDto, SaleFormRDto, SaleRDto, SaleWDto } from './pos-models';

/**
 * Poste de caisse branché sur l'API des ventes (VTE-01) : formulaire `GET /api/v1/sales/form` (canaux,
 * vendeuses, articles vendables et leur stock) et enregistrement `POST /api/v1/sales` par la file hors ligne
 * (`OutboxService`, SYS-07). Promotions et tickets en attente restent à brancher (PRO-02, VTE-04, VTE-06).
 */
@Injectable({ providedIn: 'root' })
export class PosService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly referential = inject(ReferentialService);
  private readonly authStore = inject(AuthStore);
  private readonly baseUrl = '/api/v1/sales';

  context(): Observable<PosContextRDto> {
    return forkJoin({
      form: this.http.get<SaleFormRDto>(`${this.baseUrl}/form`),
      ranges: this.referential.listProductRanges(),
    }).pipe(
      map(({ form, ranges }) =>
        toPosContext(form, ranges, this.authStore.currentUser(), new Date()),
      ),
    );
  }

  /** Enregistre la vente ; `queued` si elle attend le réseau (le serveur la numérote au rejeu). */
  record(body: SaleWDto): Observable<SubmitResult<SaleRDto>> {
    return this.outbox.submit<SaleRDto>('POST', this.baseUrl, body);
  }
}
