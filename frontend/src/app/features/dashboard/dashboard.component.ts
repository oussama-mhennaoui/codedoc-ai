import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProjectService } from '../../core/api/project.service';
import { Project } from '../../core/models/project.model';
import { ProjectFormDialogComponent } from './project-form-dialog.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatTooltipModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private projectService = inject(ProjectService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  projects = signal<Project[]>([]);
  isLoading = signal(false);
  displayedColumns: string[] = ['name', 'language', 'updatedAt', 'actions'];

  isEmpty = computed(() => this.projects().length === 0 && !this.isLoading());

  ngOnInit() {
    this.loadProjects();
  }

  loadProjects() {
    this.isLoading.set(true);
    this.projectService.list().subscribe({
      next: (data) => {
        this.projects.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open('Error loading projects', 'Close', { duration: 3000 });
      }
    });
  }

  openCreateDialog() {
    const dialogRef = this.dialog.open(ProjectFormDialogComponent, {
      width: '400px',
      data: {}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.projectService.create(result).subscribe({
          next: () => {
            this.loadProjects();
            this.snackBar.open('Project created successfully', 'Close', { duration: 3000 });
          },
          error: () => this.snackBar.open('Error creating project', 'Close', { duration: 3000 })
        });
      }
    });
  }

  openEditDialog(project: Project) {
    const dialogRef = this.dialog.open(ProjectFormDialogComponent, {
      width: '400px',
      data: { project }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.projectService.update(project.id, result).subscribe({
          next: () => {
            this.loadProjects();
            this.snackBar.open('Project updated successfully', 'Close', { duration: 3000 });
          },
          error: () => this.snackBar.open('Error updating project', 'Close', { duration: 3000 })
        });
      }
    });
  }

  archiveProject(project: Project) {
    this.projectService.archive(project.id).subscribe({
      next: () => {
        this.loadProjects();
        this.snackBar.open('Project archived', 'Close', { duration: 3000 });
      },
      error: () => this.snackBar.open('Error archiving project', 'Close', { duration: 3000 })
    });
  }

  unarchiveProject(project: Project) {
    // Assuming we can use update to unarchive
    this.projectService.update(project.id, { archived: false }).subscribe({
      next: () => {
        this.loadProjects();
        this.snackBar.open('Project unarchived', 'Close', { duration: 3000 });
      },
      error: () => this.snackBar.open('Error unarchiving project', 'Close', { duration: 3000 })
    });
  }

  deleteProject(project: Project) {
    if (confirm(`Are you sure you want to delete "${project.name}"?`)) {
      this.projectService.remove(project.id).subscribe({
        next: () => {
          this.loadProjects();
          this.snackBar.open('Project deleted', 'Close', { duration: 3000 });
        },
        error: () => this.snackBar.open('Error deleting project', 'Close', { duration: 3000 })
      });
    }
  }
}
