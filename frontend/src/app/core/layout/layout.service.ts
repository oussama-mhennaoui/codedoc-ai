import { Injectable, signal, computed } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { inject } from '@angular/core';
import { map, shareReplay } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  private breakpointObserver = inject(BreakpointObserver);

  // Is the screen small (mobile/tablet)?
  isHandset = toSignal(
    this.breakpointObserver.observe([Breakpoints.Handset, Breakpoints.TabletPortrait]).pipe(
      map(result => result.matches),
      shareReplay()
    ),
    { initialValue: false }
  );

  // Sidebar state
  isSidebarCollapsed = signal(false);
  isMobileMenuOpen = signal(false);

  toggleSidebar() {
    if (this.isHandset()) {
      this.isMobileMenuOpen.update(v => !v);
    } else {
      this.isSidebarCollapsed.update(v => !v);
    }
  }

  closeMobileMenu() {
    this.isMobileMenuOpen.set(false);
  }
}
