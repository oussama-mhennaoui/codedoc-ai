import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { ProjectService } from '../../core/api/project.service';
import { FileService } from '../../core/api/file.service';
import { TestGenerationService } from '../../core/api/test-generation.service';
import { Project } from '../../core/models/project.model';
import { SourceFile } from '../../core/models/source-file.model';
import { TestGeneration, TestFile, TestCase } from '../../core/models/test-generation.model';
import { CodeViewerComponent } from '../../shared/components/code-viewer/code-viewer.component';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-test-generator',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatChipsModule,
    MatExpansionModule,
    MatTabsModule,
    MatDividerModule,
    CodeViewerComponent
  ],
  templateUrl: './test-generator.component.html',
  styleUrl: './test-generator.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TestGeneratorComponent implements OnInit {
  private projectService = inject(ProjectService);
  private fileService = inject(FileService);
  private testGenService = inject(TestGenerationService);
  private snackBar = inject(MatSnackBar);

  // Form state
  projects = signal<Project[]>([]);
  files = signal<SourceFile[]>([]);
  selectedProjectId = signal<number | null>(null);
  selectedFileId = signal<number | null>(null);
  sourceCode = signal('');
  selectedLanguage = signal('java');
  selectedFramework = signal('');
  inputMode = signal<'file' | 'paste'>('paste');

  // Generation state
  isLoading = signal(false);
  isLoadingProjects = signal(false);
  isLoadingFiles = signal(false);
  generationResult = signal<TestGeneration | null>(null);
  selectedTestFileIndex = signal(0);

  // History
  history = signal<TestGeneration[]>([]);
  showHistory = signal(false);

  languages = [
    { value: 'java', label: 'Java' },
    { value: 'javascript', label: 'JavaScript' },
    { value: 'typescript', label: 'TypeScript' },
    { value: 'python', label: 'Python' }
  ];

  frameworksByLanguage: Record<string, {value: string, label: string}[]> = {
    java: [{ value: 'junit5', label: 'JUnit 5 + Mockito' }],
    javascript: [{ value: 'jest', label: 'Jest' }],
    typescript: [
      { value: 'jest', label: 'Jest' },
      { value: 'vitest', label: 'Vitest' }
    ],
    python: [{ value: 'pytest', label: 'pytest' }]
  };

  availableFrameworks = computed(() => {
    return this.frameworksByLanguage[this.selectedLanguage()] || [];
  });

  canGenerate = computed(() => {
    if (this.isLoading()) return false;
    if (this.inputMode() === 'file') {
      return this.selectedFileId() !== null;
    }
    return this.sourceCode().trim().length > 0;
  });

  testFiles = computed<TestFile[]>(() => {
    const result = this.generationResult();
    if (!result?.result?.testFiles) return [];
    return result.result.testFiles;
  });

  currentTestFile = computed<TestFile | null>(() => {
    const files = this.testFiles();
    const idx = this.selectedTestFileIndex();
    return files[idx] ?? null;
  });

  ngOnInit() {
    this.loadProjects();
  }

  loadProjects() {
    this.isLoadingProjects.set(true);
    this.projectService.list().subscribe({
      next: (data) => {
        this.projects.set(data);
        this.isLoadingProjects.set(false);
      },
      error: () => {
        this.isLoadingProjects.set(false);
      }
    });
  }

  onProjectChange(projectId: number) {
    this.selectedProjectId.set(projectId);
    this.selectedFileId.set(null);
    this.isLoadingFiles.set(true);
    this.fileService.listForProject(projectId).subscribe({
      next: (data) => {
        this.files.set(data);
        this.isLoadingFiles.set(false);
      },
      error: () => {
        this.isLoadingFiles.set(false);
        this.snackBar.open('Error loading files', 'Close', { duration: 3000 });
      }
    });
  }

  onFileChange(fileId: number) {
    this.selectedFileId.set(fileId);
    // Load file content to preview
    this.fileService.get(fileId).subscribe({
      next: (data) => {
        this.sourceCode.set(data.content);
        if (data.language) {
          this.selectedLanguage.set(data.language.toLowerCase());
        }
      }
    });
  }

  onLanguageChange(lang: string) {
    this.selectedLanguage.set(lang);
    // Reset framework to default for this language
    const frameworks = this.frameworksByLanguage[lang];
    this.selectedFramework.set(frameworks?.[0]?.value || '');
  }

  generate() {
    if (!this.canGenerate()) return;

    this.isLoading.set(true);
    this.generationResult.set(null);
    this.selectedTestFileIndex.set(0);

    const request: any = {
      language: this.selectedLanguage(),
      testFramework: this.selectedFramework() || undefined
    };

    if (this.inputMode() === 'file' && this.selectedFileId()) {
      request.fileId = this.selectedFileId();
    } else {
      request.sourceCode = this.sourceCode();
    }

    this.testGenService.generate(request).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (result) => {
        this.generationResult.set(result);
        this.snackBar.open('Tests generated successfully!', 'Close', { duration: 3000 });
      },
      error: (err) => {
        const status = err?.status || 0;
        let msg = 'Failed to generate tests';

        if (status === 0) {
          msg = 'Network error: Cannot reach the server';
        } else if (status === 429) {
          msg = 'AI quota exceeded. Please try again later.';
        } else if (status === 502) {
          msg = 'AI service is currently unavailable.';
        } else if (status === 504) {
          msg = 'AI request timed out. The code might be too large.';
        } else if (status === 422) {
          msg = 'Invalid input. Please check your code and try again.';
        } else if (err?.error?.message) {
          msg = err.error.message;
        }

        this.snackBar.open(msg, 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
      }
    });
  }

  copyCode(content: string) {
    navigator.clipboard.writeText(content).then(() => {
      this.snackBar.open('Code copied to clipboard!', 'Close', { duration: 2000 });
    });
  }

  downloadFile(testFile: TestFile) {
    const blob = new Blob([testFile.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = testFile.filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  downloadAll() {
    const result = this.generationResult();
    if (!result) return;

    this.testGenService.download(result.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const files = this.testFiles();
        a.download = files.length === 1 ? files[0].filename : 'generated-tests.zip';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.snackBar.open('Error downloading files', 'Close', { duration: 3000 });
      }
    });
  }

  loadHistory() {
    this.showHistory.set(!this.showHistory());
    if (this.showHistory() && this.history().length === 0) {
      this.testGenService.getHistory().subscribe({
        next: (data) => this.history.set(data),
        error: () => this.snackBar.open('Error loading history', 'Close', { duration: 3000 })
      });
    }
  }

  loadFromHistory(item: TestGeneration) {
    this.generationResult.set(item);
    this.selectedTestFileIndex.set(0);
    this.showHistory.set(false);
  }

  getCategoryColor(category: string): string {
    switch (category) {
      case 'happy_path': return 'primary';
      case 'edge_case': return 'accent';
      case 'error_handling': return 'warn';
      case 'null_safety': return 'warn';
      case 'integration': return 'primary';
      default: return '';
    }
  }

  getCategoryLabel(category: string): string {
    switch (category) {
      case 'happy_path': return 'Happy Path';
      case 'edge_case': return 'Edge Case';
      case 'error_handling': return 'Error Handling';
      case 'null_safety': return 'Null Safety';
      case 'integration': return 'Integration';
      default: return category;
    }
  }
}
