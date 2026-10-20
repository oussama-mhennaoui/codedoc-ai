import { Component, inject, signal, computed } from '@angular/core';
import { RouterOutlet, RouterModule, Router, NavigationEnd } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from './core/auth/auth.service';
import { LayoutService } from './core/layout/layout.service';
import { SidebarComponent } from './core/layout/sidebar/sidebar.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    MatTooltipModule,
    SidebarComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  authService = inject(AuthService);
  layoutService = inject(LayoutService);
  private router = inject(Router);

  currentUrl = signal(this.router.url);

  isOnLandingPage = computed(() => {
    const url = this.currentUrl();
    return url === '/' || url === '' || url.startsWith('/#') || url.startsWith('/?');
  });

  isAuthPage = computed(() => {
    const url = this.currentUrl();
    return url.startsWith('/login') || url.startsWith('/register');
  });

  isFullBleedPage = computed(() => {
    return this.isOnLandingPage() || this.isAuthPage();
  });

  isObservabilityPage = computed(() => {
    const url = this.currentUrl();
    return url.includes('/ai-observability');
  });

  pageTitle = computed(() => {
    const url = this.currentUrl();
    if (url.includes('/ai-observability')) return 'AI Observability Studio';
    if (url.includes('/dashboard')) return 'Dashboard';
    if (url.includes('/unit-test-generator')) return 'AI Test Generator';
    if (url.includes('/audit')) return 'Security Audit';
    if (url.includes('/files/')) return 'File Explorer';
    if (url.includes('/projects/')) return 'Project Workspace';
    if (url.includes('/docs/')) return 'Documentation Studio';
    return 'Dashboard';
  });

  constructor() {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.currentUrl.set(event.urlAfterRedirects);
      }
    });
  }

  isLandingPage() {
    return this.isOnLandingPage();
  }


  logout() {
    this.authService.logout();
  }
}

