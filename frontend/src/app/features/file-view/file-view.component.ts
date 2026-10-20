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
import { MatTabsModule } from '@angular/material/tabs';
import { FileService } from '../../core/api/file.service';
import { AiService } from '../../core/api/ai.service';
import { DocService } from '../../core/api/doc.service';
import { QualityService } from '../../core/api/quality.service';
import { SourceFileDetail } from '../../core/models/source-file.model';
import { GeneratedDoc } from '../../core/models/doc.model';
import { QualityReport } from '../../core/models/quality.model';
import { CodeViewerComponent } from '../../shared/components/code-viewer/code-viewer.component';
import { QualityAnalysisPanelComponent } from '../../shared/components/quality-analysis-panel/quality-analysis-panel.component';
import { ChatbotComponent } from '../chatbot/chatbot.component';
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
    MatTabsModule,
    CodeViewerComponent,
    QualityAnalysisPanelComponent,
    ChatbotComponent
  ],
  templateUrl: './file-view.component.html',
  styleUrl: './file-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FileViewComponent implements OnInit {
  private fileService = inject(FileService);
  private aiService = inject(AiService);
  private qualityService = inject(QualityService);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private docService = inject(DocService);

  id = input.required<string>(); // Project ID
  fileId = input.required<string>(); // File ID

  file = signal<SourceFileDetail | null>(null);
  isLoading = signal(false);
  isAiLoading = signal(false);
  isQualityLoading = signal(false);
  qualityReport = signal<QualityReport | null>(null);
  docId = signal<string | null>(null);
  selectedTabIndex = signal<number>(0);
  // Module 3 — Chatbot
  explainQuestion = signal<string | null>(null);

  ngOnInit() {
    this.loadFile();
    this.checkExistingDoc();
    this.loadQualityReport();
  }

  checkExistingDoc() {
    const projectId = this.id();
    const fileId = this.fileId();

    this.docService.listByProject(projectId).subscribe({
      next: (docs: GeneratedDoc[]) => {
        const existing = docs.find(d => String(d.sourceFileId) === fileId);
        if (existing) {
          this.docId.set(existing.id);
        }
      }
    });
  }

  loadQualityReport() {
    const fId = parseInt(this.fileId(), 10);
    if (isNaN(fId)) return;

    this.qualityService.getReportByFile(fId).subscribe({
      next: (report: QualityReport) => {
        this.qualityReport.set(report);
      },
      error: () => {
        this.qualityReport.set(null);
      }
    });
  }

  triggerQualityAnalysis() {
    const fId = parseInt(this.fileId(), 10);
    if (isNaN(fId)) return;

    this.isQualityLoading.set(true);
    this.qualityService.analyze(fId).pipe(
      finalize(() => this.isQualityLoading.set(false))
    ).subscribe({
      next: (report: QualityReport) => {
        this.qualityReport.set(report);
        this.selectedTabIndex.set(1); // switch to Quality tab
        this.snackBar.open('Analyse de qualité terminée avec succès !', 'Fermer', { duration: 4000 });
      },
      error: (err) => this.handleAiError(err)
    });
  }

  exportPdf() {
    const report = this.qualityReport();
    const currentFile = this.file();
    if (!report || !currentFile) return;
    this.qualityService.downloadReport(report.id, currentFile.filename, 'pdf');
  }

  exportMarkdown() {
    const report = this.qualityReport();
    const currentFile = this.file();
    if (!report || !currentFile) return;
    this.qualityService.downloadReport(report.id, currentFile.filename, 'md');
  }

  onLineSelected(line: number) {
    this.selectedTabIndex.set(0); // Switch to code viewer tab
    this.snackBar.open(`Navigation vers la ligne ${line}`, 'OK', { duration: 2500 });
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
    this.aiService.generateDoc({ fileId: fId }).subscribe({
      next: (resp) => {
        this.isAiLoading.set(false);
        this.snackBar.open('Documentation generated successfully!', 'Open', { duration: 5000 })
          .onAction().subscribe(() => this.router.navigate(['/docs', resp.docId]));
        this.checkExistingDoc();
      },
      error: () => {
        this.isAiLoading.set(false);
        this.snackBar.open('Error generating documentation', 'Close', { duration: 3000 });
      }
    });
  }

  // Module 3 — Lance le chatbot avec une question pré-remplie sur ce fichier
  explainWithAI() {
    const filename = this.file()?.filename ?? 'ce fichier';
    this.explainQuestion.set(`Explique la logique et les fonctions principales de ${filename}`);
  }
}
