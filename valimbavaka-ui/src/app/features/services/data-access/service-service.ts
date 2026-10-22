import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { SERVICES_FIXTURE } from './service-fixtures';
import { SalonServiceWDto, ServicesContextRDto } from './service-models';

/**
 * Salon et shooting. Provisoire ; à terme `GET /api/v1/services/context`, `POST /api/v1/services`
 * et `POST /api/v1/shootings/{id}/return`.
 */
@Injectable({ providedIn: 'root' })
export class SalonService {
  context(): Observable<ServicesContextRDto> {
    return demo(SERVICES_FIXTURE);
  }

  register(service: SalonServiceWDto): Observable<SalonServiceWDto> {
    return demo(service);
  }

  /** Simulation : réintègre les produits d'un shooting au stock. */
  returnShooting(number: string): Observable<string> {
    return demo(number).pipe(map((value) => value));
  }
}
