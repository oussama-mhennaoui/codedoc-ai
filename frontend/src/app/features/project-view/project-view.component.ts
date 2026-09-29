import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProjectService } from '../../core/api/project.service';
import { FileService } from '../../core/api/file.service';
import { DocService } from '../../core/api/doc.service';
import { Project } from '../../core/models/project.model';
import { SourceFile } from '../../core/models/source-file.model';
import { GeneratedDoc } from '../../core/models/doc.model';
import { ProjectFormDialogComponent } from '../dashboard/project-form-dialog.component';
import { FileUploadDialogComponent } from './file-upload-dialog.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-project-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatCardModule,
    MatTabsModule,
    MatTableModule,
    MatTooltipModule
  ],
  templateUrl: './project-view.component.html',
  styleUrl: './project-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectViewComponent implements OnInit {
  private projectService = inject(ProjectService);
  private fileService = inject(FileService);
  private docService = inject(DocService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  id = input.required<string>(); // Route parameter bound via withComponentInputBinding()
  
  project = signal<Project | null>(null);
  files = signal<SourceFile[]>([]);
  docs = signal<GeneratedDoc[]>([]);
  isLoading = signal(false);
  isFilesLoading = signal(false);
  isDocsLoading = signal(false);
  
  displayedColumns: string[] = ['filename', 'language', 'size', 'uploadedAt', 'actions'];
  docsColumns: string[] = ['filename', 'status', 'model', 'createdAt', 'actions'];

  ngOnInit() {
    this.loadProject();
    this.loadFiles();
    this.loadDocs();
  }

  loadProject() {
    const projectId = parseInt(this.id(), 10);
    if (isNaN(projectId)) return;

    this.isLoading.set(true);
    this.projectService.get(projectId).subscribe({
      next: (data) => {
        this.project.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open('Error loading project', 'Close', { duration: 3000 });
      }
    });
  }

  loadFiles() {
    const projectId = parseInt(this.id(), 10);
    if (isNaN(projectId)) return;

    this.isFilesLoading.set(true);
    this.fileService.listForProject(projectId).subscribe({
      next: (data) => {
        this.files.set(data);
        this.isFilesLoading.set(false);
      },
      error: () => {
        this.isFilesLoading.set(false);
        this.snackBar.open('Error loading files', 'Close', { duration: 3000 });
      }
    });
  }

  openEditDialog() {
    const currentProject = this.project();
    if (!currentProject) return;

    const dialogRef = this.dialog.open(ProjectFormDialogComponent, {
      width: '400px',
      data: { project: currentProject }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.projectService.update(currentProject.id, result).subscribe({
          next: (updated) => {
            this.project.set(updated);
            this.snackBar.open('Project updated successfully', 'Close', { duration: 3000 });
          },
          error: () => this.snackBar.open('Error updating project', 'Close', { duration: 3000 })
        });
      }
    });
  }

  openUploadDialog() {
    const dialogRef = this.dialog.open(FileUploadDialogComponent, {
      width: '600px',
      data: { language: this.project()?.language }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const projectId = parseInt(this.id(), 10);
        this.fileService.create(projectId, result).subscribe({
          next: () => {
            this.loadFiles();
            this.snackBar.open('File uploaded successfully', 'Close', { duration: 3000 });
          },
          error: (error) => {
            const message = error.status === 409 ? 'File already exists' : 'Error uploading file';
            this.snackBar.open(message, 'Close', { duration: 3000 });
          }
        });
      }
    });
  }

  loadDocs() {
    this.isDocsLoading.set(true);
    this.docService.listByProject(this.id()).pipe(
    ).subscribe({
      next: (data) => {
        this.docs.set(data);
        this.isDocsLoading.set(false);
      },
      error: () => {
        this.isDocsLoading.set(false);
      }
    });
  }

  viewDoc(docId: string) {
    this.router.navigate(['/docs', docId]);
  }

  viewFile(fileId: number) {
    this.router.navigate(['/projects', this.id(), 'files', fileId]);
  }

  deleteFile(event: Event, fileId: number) {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this file?')) {
      this.fileService.remove(fileId).subscribe({
        next: () => {
          this.loadFiles();
          this.snackBar.open('File deleted', 'Close', { duration: 3000 });
        },
        error: () => this.snackBar.open('Error deleting file', 'Close', { duration: 3000 })
      });
    }
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
