import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { SecurityService, SecurityAudit, Vulnerability, Severity } from '../../core/api/security.service';
import { FileService } from '../../core/api/file.service';
import { SourceFileDetail } from '../../core/models/source-file.model';
import { CodeViewerComponent } from '../../shared/components/code-viewer/code-viewer.component';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-security-audit',
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
    MatExpansionModule,
    MatChipsModule,
    CodeViewerComponent
  ],
  templateUrl: './security-audit.component.html',
  styleUrl: './security-audit.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SecurityAuditComponent implements OnInit {
  private securityService = inject(SecurityService);
  private fileService = inject(FileService);
  private snackBar = inject(MatSnackBar);

  id = input.required<string>(); // Project ID
  fileId = input.required<string>(); // File ID
  
  file = signal<SourceFileDetail | null>(null);
  audit = signal<SecurityAudit | null>(null);
  isLoading = signal(false);
  isAiLoading = signal(false);
  showGuide = signal(false);

  glossary = [
    { 
      term: 'CWE (Common Weakness Enumeration)', 
      definition: 'C\'est un dictionnaire universel des faiblesses logicielles. Chaque numéro (ex: CWE-89) correspond à un type précis de faille de sécurité connu dans le monde du développement.' 
    },
    { 
      term: 'OWASP Top 10', 
      definition: 'C\'est la liste des 10 risques de sécurité les plus critiques pour les applications web. C\'est le standard mondial pour les développeurs.' 
    },
    { 
      term: 'Score de Sécurité (A-F)', 
      definition: 'Une note globale calculée par l\'IA. A est excellent (sécurisé), tandis que F signifie que le code contient des failles critiques qui doivent être corrigées immédiatement.' 
    },
    { 
      term: 'Sévérité', 
      definition: 'Indique l\'urgence de la correction. CRITICAL et HIGH demandent une action immédiate, tandis que LOW et INFO sont des recommandations d\'amélioration.' 
    }
  ];

  ngOnInit() {
    this.loadFile();
    this.loadLatestAudit();
  }

  loadFile() {
    const fId = parseInt(this.fileId(), 10);
    if (isNaN(fId)) return;

    this.isLoading.set(true);
    this.fileService.get(fId).subscribe({
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

  loadLatestAudit() {
    const fId = parseInt(this.fileId(), 10);
    if (isNaN(fId)) return;

    this.securityService.getFileAudits(fId).subscribe({
      next: (audits) => {
        if (audits.length > 0) {
          this.audit.set(audits[0]);
        }
      }
    });
  }

  performAudit() {
    const fId = parseInt(this.fileId(), 10);
    if (isNaN(fId)) return;

    this.isAiLoading.set(true);
    this.securityService.performAudit({ fileId: fId }).pipe(
      finalize(() => this.isAiLoading.set(false))
    ).subscribe({
      next: (audit) => {
        this.audit.set(audit);
        this.snackBar.open('Security audit completed successfully', 'Close', { duration: 3000 });
      },
      error: (err) => this.handleAiError(err)
    });
  }

  exportPdf() {
    const currentAudit = this.audit();
    if (!currentAudit) return;

    this.securityService.exportPdf(currentAudit.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `security-audit-${currentAudit.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.snackBar.open('Error exporting PDF', 'Close', { duration: 3000 });
      }
    });
  }

  getSeverityColor(severity: Severity): string {
    switch (severity) {
      case Severity.CRITICAL: return 'error-bg';
      case Severity.HIGH: return 'warn-bg';
      case Severity.MEDIUM: return 'accent-bg';
      case Severity.LOW: return 'info-bg';
      default: return 'default-bg';
    }
  }

  getScoreColor(score: string): string {
    if (['A', 'B'].includes(score)) return 'score-good';
    if (['C', 'D'].includes(score)) return 'score-medium';
    return 'score-bad';
  }

  private handleAiError(err: any) {
    const status = err?.status || 0;
    let msg = 'Failed to perform security audit';
    
    if (status === 429) msg = 'Quota exceeded. Please try again later.';
    else if (status === 504) msg = 'AI request timed out.';
    
    this.snackBar.open(msg, 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
  }
}
