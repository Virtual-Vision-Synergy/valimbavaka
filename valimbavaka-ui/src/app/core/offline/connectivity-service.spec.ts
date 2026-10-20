import { TestBed } from '@angular/core/testing';
import { ConnectivityService } from './connectivity-service';

describe('ConnectivityService', () => {
  it('should follow the browser online and offline events', () => {
    const sut = TestBed.inject(ConnectivityService);

    window.dispatchEvent(new Event('offline'));
    const afterOffline = sut.isOnline();
    window.dispatchEvent(new Event('online'));

    expect(afterOffline).toBe(false);
    expect(sut.isOnline()).toBe(true);
  });
});
