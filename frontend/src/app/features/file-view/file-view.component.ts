import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FileService } from '../../core/api/file.service';
import { AiService } from '../../core/api/ai.service';
import { SourceFileDetail } from '../../core/models/source-file.model';
import { CodeViewerComponent } from '../../shared/components/code-viewer/code-viewer.component';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-file-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatCardModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    CodeViewerComponent
  ],
  templateUrl: './file-view.component.html',
  styleUrl: './file-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FileViewComponent implements OnInit {
  private fileService = inject(FileService);
  private aiService = inject(AiService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  id = input.required<string>(); // Project ID
  fileId = input.required<string>(); // File ID
  
  file = signal<SourceFileDetail | null>(null);
  isLoading = signal(false);
  isAiLoading = signal(false);
  complexityResult = signal<{score: string, details: string} | null>(null);

  @ViewChild('complexityDialog') complexityDialog!: TemplateRef<any>;

  ngOnInit() {
    this.loadFile();
  }

  loadFile() {
    const fileId = parseInt(this.fileId(), 10);
    if (isNaN(fileId)) return;

    this.isLoading.set(true);
    this.fileService.get(fileId).subscribe({
      next: (data) => {
        this.file.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open('Error loading file', 'Close', { duration: 3000 });
      }
    });
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  private handleAiError(err: any) {
    const status = err?.status || 0;
    const errorBody = err?.error || {};
    
    let msg = 'Failed to process AI request';
    
    if (status === 0) {
      msg = 'Network error: Cannot reach the server';
    } else if (status === 429) {
      msg = 'Quota exceeded. Please try again later.';
    } else if (status === 502) {
      msg = 'AI service is currently unavailable.';
    } else if (status === 504) {
      msg = 'AI request timed out. The file might be too large.';
    } else if (status === 500 && errorBody.error === 'missing_api_key') {
      msg = 'Server configuration error: Missing AI API key.';
    }
    
    this.snackBar.open(msg, 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
  }

  generateDoc() {
    const fId = parseInt(this.fileId(), 10);
    if (isNaN(fId)) return;

    this.isAiLoading.set(true);
    this.aiService.generateDoc({ fileId: fId }).pipe(
      finalize(() => this.isAiLoading.set(false))
    ).subscribe({
      next: (res: any) => {
        const data = res.data || {};
        const docId = data.docId;
        if (!docId) {
          this.snackBar.open('Documentation generated but no ID returned. Please rebuild the backend.', 'Close', { duration: 6000, panelClass: ['error-snackbar'] });
          return;
        }
        this.snackBar.open('Documentation generated successfully', 'Close', { duration: 3000 });
        this.router.navigate(['/docs', docId]);
      },
      error: (err) => this.handleAiError(err)
    });
  }

  detectComplexity() {
    const fId = parseInt(this.fileId(), 10);
    if (isNaN(fId)) return;

    this.isAiLoading.set(true);
    this.complexityResult.set(null);
    this.aiService.detectComplexity({ fileId: fId }).pipe(
      finalize(() => this.isAiLoading.set(false))
    ).subscribe({
      next: (res: any) => {
        const data = res.data || res || {};
        const score = data.complexityScore ?? data.score ?? 'N/A';
        const details = data.overallAssessment ?? data.details ?? 'No details returned';
        this.complexityResult.set({ score: String(score), details });
        this.dialog.open(this.complexityDialog, { width: '500px' });
      },
      error: (err) => this.handleAiError(err)
    });
  }
}

