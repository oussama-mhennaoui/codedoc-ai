import { Component, inject, computed, signal, effect, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LayoutService } from '../layout.service';
import { AuthService } from '../../auth/auth.service';
import { ProjectService } from '../../api/project.service';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  exact?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  layoutService = inject(LayoutService);
  authService = inject(AuthService);
  private router = inject(Router);
  private projectService = inject(ProjectService);

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', exact: true },
    { label: 'Test Generator', icon: 'science', route: '/unit-test-generator' },
  ];

  // Detect active project from route
  activeProjectId = toSignal(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => {
        const url = this.router.url;
        const projectMatch = url.match(/\/projects\/(\d+)/);
        return projectMatch ? projectMatch[1] : null;
      })
    )
  );

  // Detect the active file if needed
  activeFileId = toSignal(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => {
        const url = this.router.url;
        const fileMatch = url.match(/\/files\/(\d+)/);
        return fileMatch ? fileMatch[1] : null;
      })
    )
  );

  activeProjectName = signal<string | null>(null);

  constructor() {
    effect(() => {
      const id = this.activeProjectId();
      if (id) {
        this.projectService.get(parseInt(id, 10)).subscribe(p => this.activeProjectName.set(p.name));
      } else {
        this.activeProjectName.set(null);
      }
    });
  }

  logout() {
    this.authService.logout();
  }
}
