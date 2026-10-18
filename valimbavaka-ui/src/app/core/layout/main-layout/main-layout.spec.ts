import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MainLayout } from './main-layout';

describe('MainLayout', () => {
  it('should expose the main navigation with a link to users', async () => {
    await TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(MainLayout);
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    const nav = element.querySelector('nav[aria-label="Navigation principale"]');
    expect(nav?.textContent).toContain('Utilisateurs');
    expect(element.querySelector('main')).not.toBeNull();
  });
});
