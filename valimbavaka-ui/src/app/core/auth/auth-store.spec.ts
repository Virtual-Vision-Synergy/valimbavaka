import { TestBed } from '@angular/core/testing';
import { AuthStore } from './auth-store';
import { buildUser } from './auth-testing';

describe('AuthStore', () => {
  let sut: AuthStore;

  beforeEach(() => {
    sut = TestBed.inject(AuthStore);
  });

  it('should be unauthenticated and grant nothing before sign in', () => {
    expect(sut.isAuthenticated()).toBe(false);
    expect(sut.hasPermission('dashboard.read')).toBe(false);
  });

  it('should grant exactly the permissions of the signed in role', () => {
    sut.signIn('jeton', buildUser('DELIVERY_DRIVER'));

    expect(sut.isAuthenticated()).toBe(true);
    expect(sut.currentUser()?.role).toBe('DELIVERY_DRIVER');
    expect(sut.hasPermission('deliveries.read')).toBe(true);
    expect(sut.hasPermission('users.read')).toBe(false);
  });

  it('should forget the user and the token on sign out', () => {
    sut.signIn('jeton', buildUser('ADMIN'));

    sut.signOut();

    expect(sut.isAuthenticated()).toBe(false);
    expect(sut.currentUser()).toBeNull();
    expect(sut.hasPermission('users.read')).toBe(false);
  });
});
