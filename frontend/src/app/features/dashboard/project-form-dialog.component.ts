import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Project } from '../../core/models/project.model';

@Component({
  selector: 'app-project-form-dialog',
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
      <mat-icon>{{ data.project ? 'edit' : 'add_circle' }}</mat-icon>
      {{ data.project ? 'Edit Project' : 'Create New Project' }}
    </h2>
    <mat-dialog-content class="dialog-content">
      <form [formGroup]="projectForm" id="projectForm" (ngSubmit)="onSubmit()">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Project Name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. My Awesome API">
          <mat-icon matPrefix>folder</mat-icon>
          @if (projectForm.controls.name.errors?.['required']) {
            <mat-error>Name is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" placeholder="Briefly describe your project" rows="3"></textarea>
          <mat-icon matPrefix>description</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Language</mat-label>
          <mat-select formControlName="language">
            <mat-select-trigger>
              <div class="select-trigger">
                <mat-icon>code</mat-icon>
                <span>{{ projectForm.controls.language.value }}</span>
              </div>
            </mat-select-trigger>
            @for (lang of languages; track lang) {
              <mat-option [value]="lang">{{ lang }}</mat-option>
            }
          </mat-select>
          <mat-icon matPrefix>code</mat-icon>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" type="submit" form="projectForm" [disabled]="projectForm.invalid">
        <mat-icon>{{ data.project ? 'save' : 'add' }}</mat-icon>
        {{ data.project ? 'Save Changes' : 'Create Project' }}
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
      min-width: 350px;
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
export class ProjectFormDialogComponent {
  private dialogRef = inject(MatDialogRef<ProjectFormDialogComponent>);
  public data = inject<{ project?: Project }>(MAT_DIALOG_DATA);

  languages = ['Java', 'TypeScript', 'JavaScript', 'Python', 'C#', 'Go', 'Rust'];

  projectForm = new FormGroup({
    name: new FormControl(this.data.project?.name ?? '', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl(this.data.project?.description ?? '', { nonNullable: true }),
    language: new FormControl(this.data.project?.language ?? 'Java', { nonNullable: true })
  });

  onSubmit() {
    if (this.projectForm.valid) {
      this.dialogRef.close(this.projectForm.getRawValue());
    }
  }
}
