import { Component, inject, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LayoutService } from '../layout.service';
import { AuthService } from '../../auth/auth.service';
import { ProjectService } from '../../api/project.service';
import { Project } from '../../models/project.model';
import { ProjectFormDialogComponent } from '../../../features/dashboard/project-form-dialog.component';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: string;
  exact?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule, MatTooltipModule, MatDialogModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  layoutService = inject(LayoutService);
  authService = inject(AuthService);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private dialog = inject(MatDialog);

  projects = signal<Project[]>([]);
  isLoadingProjects = signal(false);

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', exact: true },
    { label: 'AI Test Generator', icon: 'science', route: '/unit-test-generator', badge: 'AI' },
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
    ),
    { initialValue: this.getInitialProjectId() }
  );

  // Detect active file if inside a file/audit view
  activeFileId = toSignal(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => {
        const url = this.router.url;
        const fileMatch = url.match(/\/files\/(\d+)/);
        return fileMatch ? fileMatch[1] : null;
      })
    ),
    { initialValue: this.getInitialFileId() }
  );

  activeProject = signal<Project | null>(null);

  constructor() {
    this.loadProjects();

    effect(() => {
      const id = this.activeProjectId();
      if (id) {
        const found = this.projects().find(p => p.id === parseInt(id, 10));
        if (found) {
          this.activeProject.set(found);
        } else {
          this.projectService.get(parseInt(id, 10)).subscribe({
            next: (p) => this.activeProject.set(p),
            error: () => this.activeProject.set(null)
          });
        }
      } else {
        this.activeProject.set(null);
      }
    });
  }

  private getInitialProjectId(): string | null {
    const match = this.router.url.match(/\/projects\/(\d+)/);
    return match ? match[1] : null;
  }

  private getInitialFileId(): string | null {
    const match = this.router.url.match(/\/files\/(\d+)/);
    return match ? match[1] : null;
  }

  loadProjects() {
    this.isLoadingProjects.set(true);
    this.projectService.list().subscribe({
      next: (data) => {
        this.projects.set(data);
        this.isLoadingProjects.set(false);
      },
      error: () => this.isLoadingProjects.set(false)
    });
  }

  openNewProjectDialog() {
    const dialogRef = this.dialog.open(ProjectFormDialogComponent, {
      width: '520px',
      data: {}
    });

    dialogRef.afterClosed().subscribe(created => {
      if (created) {
        this.loadProjects();
        this.router.navigate(['/projects', created.id]);
      }
    });
  }

  logout() {
    this.authService.logout();
  }
}

