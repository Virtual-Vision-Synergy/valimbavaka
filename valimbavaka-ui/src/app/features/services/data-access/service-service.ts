import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { SERVICES_FIXTURE } from './service-fixtures';
import { ServiceRecordWDto, ServicesContextRDto } from './service-models';

/**
 * Salon (démonstration) : à terme `GET /api/v1/services/context` et `POST /api/v1/services`. Les sorties
 * shooting et leur réintégration passent par `ShootingService` (SHO-01, SHO-02).
 */
@Injectable({ providedIn: 'root' })
export class SalonService {
  context(): Observable<ServicesContextRDto> {
    return demo(SERVICES_FIXTURE);
  }

  register(service: ServiceRecordWDto): Observable<ServiceRecordWDto> {
    return demo(service);
  }
}
