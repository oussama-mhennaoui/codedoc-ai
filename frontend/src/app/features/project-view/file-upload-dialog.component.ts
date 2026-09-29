import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SourceFile } from '../../core/models/source-file.model';

@Component({
  selector: 'app-file-upload-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon>upload_file</mat-icon>
      Upload Source File
    </h2>
    <mat-dialog-content class="dialog-content">
      <!-- File Drop Zone -->
      <div class="drop-zone" 
           [class.dragging]="isDragging"
           (dragover)="onDragOver($event)" 
           (dragleave)="onDragLeave($event)" 
           (drop)="onDrop($event)"
           (click)="fileInput.click()">
        <mat-icon class="upload-icon">cloud_upload</mat-icon>
        <p>Drag and drop your file here, or <span>browse</span></p>
        <p class="file-hint">Supports .java, .ts, .js, .py, .cs, .go, .rs, .html, .css, .json, .sql, .md</p>
        <input #fileInput type="file" (change)="onFileSelected($event)" style="display: none">
      </div>

      <div class="divider">
        <span>OR PASTE CODE</span>
      </div>

      <form [formGroup]="fileForm" id="fileForm" (ngSubmit)="onSubmit()">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Filename</mat-label>
          <input matInput formControlName="filename" placeholder="e.g. UserService.java">
          <mat-icon matPrefix>insert_drive_file</mat-icon>
          @if (fileForm.controls.filename.errors?.['required']) {
            <mat-error>Filename is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Language</mat-label>
          <mat-select formControlName="language">
            <mat-select-trigger>
              <div class="select-trigger">
                <mat-icon>code</mat-icon>
                <span>{{ fileForm.controls.language.value }}</span>
              </div>
            </mat-select-trigger>
            @for (lang of languages; track lang) {
              <mat-option [value]="lang">{{ lang }}</mat-option>
            }
          </mat-select>
          <mat-icon matPrefix>code</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Content</mat-label>
          <textarea matInput formControlName="content" placeholder="Paste your source code here..." rows="8" class="code-textarea"></textarea>
          @if (fileForm.controls.content.errors?.['required']) {
            <mat-error>Content is required</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" type="submit" form="fileForm" [disabled]="fileForm.invalid">
        <mat-icon>cloud_upload</mat-icon>
        Upload File
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 12px;
      font-weight: 600;
      padding-bottom: 16px;
      border-bottom: 1px solid #edf2f7;
      
      mat-icon {
        color: #3f51b5;
      }
    }
    .dialog-content {
      padding-top: 24px !important;
      min-width: 550px;
    }
    
    /* Drop Zone Styles */
    .drop-zone {
      border: 2px dashed #cbd5e0;
      border-radius: 12px;
      padding: 32px;
      text-align: center;
      cursor: pointer;
      transition: all 0.2s ease;
      background-color: #f8fafc;
      margin-bottom: 24px;

      &:hover, &.dragging {
        border-color: #3f51b5;
        background-color: #ebf4ff;
      }

      .upload-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: #718096;
        margin-bottom: 12px;
      }

      p {
        margin: 0;
        color: #4a5568;
        font-weight: 500;
        
        span {
          color: #3f51b5;
          text-decoration: underline;
        }
      }

      .file-hint {
        font-size: 0.8rem;
        color: #a0aec0;
        margin-top: 8px;
        font-weight: normal;
      }
    }

    .divider {
      display: flex;
      align-items: center;
      margin-bottom: 24px;
      color: #a0aec0;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.05em;

      &::before, &::after {
        content: "";
        flex: 1;
        height: 1px;
        background-color: #e2e8f0;
      }

      span {
        padding: 0 16px;
      }
    }

    .full-width {
      width: 100%;
      margin-bottom: 8px;
    }
    .select-trigger {
      display: flex;
      align-items: center;
      gap: 8px;
      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        margin: 0;
      }
    }
    .code-textarea {
      font-family: 'Fira Code', 'Courier New', Courier, monospace;
      font-size: 13px;
    }
    .dialog-actions {
      padding: 16px 24px !important;
      border-top: 1px solid #edf2f7;
      
      button {
        border-radius: 8px;
        padding: 0 20px;
        
        mat-icon {
          margin-right: 4px;
        }
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FileUploadDialogComponent {
  private dialogRef = inject(MatDialogRef<FileUploadDialogComponent>);
  public data = inject<{ language?: string }>(MAT_DIALOG_DATA);

  languages = ['Java', 'TypeScript', 'JavaScript', 'Python', 'C#', 'Go', 'Rust', 'HTML', 'CSS', 'JSON', 'SQL', 'Markdown'];
  isDragging = false;

  fileForm = new FormGroup({
    filename: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    language: new FormControl(this.data.language ?? 'Java', { nonNullable: true }),
    content: new FormControl('', { nonNullable: true, validators: [Validators.required] })
  });

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  private handleFile(file: File) {
    // 1. Set filename
    this.fileForm.patchValue({ filename: file.name });

    // 2. Auto-detect language from extension
    const extension = file.name.split('.').pop()?.toLowerCase();
    const languageMap: Record<string, string> = {
      'java': 'Java',
      'ts': 'TypeScript',
      'js': 'JavaScript',
      'py': 'Python',
      'cs': 'C#',
      'go': 'Go',
      'rs': 'Rust',
      'html': 'HTML',
      'css': 'CSS',
      'json': 'JSON',
      'sql': 'SQL',
      'md': 'Markdown'
    };

    if (extension && languageMap[extension]) {
      this.fileForm.patchValue({ language: languageMap[extension] });
    }

    // 3. Read content
    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target?.result as string;
      this.fileForm.patchValue({ content });
    };
    reader.readAsText(file);
  }

  onSubmit() {
    if (this.fileForm.valid) {
      this.dialogRef.close(this.fileForm.getRawValue());
    }
  }
}
