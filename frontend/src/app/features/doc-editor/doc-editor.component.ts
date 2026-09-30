import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { DocService } from '../../core/api/doc.service';
import { GeneratedDoc, DocSection, Comment } from '../../core/models/doc.model';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-doc-editor',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DragDropModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatMenuModule
  ],
  templateUrl: './doc-editor.component.html',
  styleUrls: ['./doc-editor.component.scss']
})
export class DocEditorComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private docService = inject(DocService);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);

  docId = signal<string>('');
  doc = signal<GeneratedDoc | null>(null);
  sections = signal<DocSection[]>([]);
  selectedSection = signal<DocSection | null>(null);
  comments = signal<Comment[]>([]);
  
  isLoading = signal<boolean>(true);
  isSaving = signal<boolean>(false);

  editorForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    content: ['', Validators.required]
  });

  commentForm = this.fb.nonNullable.group({
    content: ['', Validators.required]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.docId.set(id);
      this.loadData(id);
    }
  }

  loadData(id: string) {
    this.isLoading.set(true);
    this.docService.getDoc(id).subscribe({
      next: (d) => {
        this.doc.set(d);
        this.loadSections(id);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.showError('Failed to load document');
      }
    });
  }

  loadSections(docId: string) {
    this.docService.listSections(docId).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (secs) => {
        const sorted = secs.sort((a, b) => a.orderIndex - b.orderIndex);
        this.sections.set(sorted);
        if (sorted.length > 0 && !this.selectedSection()) {
          this.selectSection(sorted[0]);
        }
      },
      error: () => this.showError('Failed to load sections')
    });
  }

  selectSection(section: DocSection) {
    this.selectedSection.set(section);
    this.editorForm.patchValue({
      title: section.title,
      content: section.content
    });
    this.loadComments(this.docId(), section.id);
  }

  loadComments(docId: string, sectionId: string) {
    this.docService.listComments(docId, sectionId).subscribe({
      next: (comms) => this.comments.set(comms),
      error: () => this.showError('Failed to load comments')
    });
  }

  saveSection() {
    if (this.editorForm.invalid || !this.selectedSection()) return;

    this.isSaving.set(true);
    const formVal = this.editorForm.getRawValue();
    const sec = this.selectedSection()!;

    this.docService.updateSection(this.docId(), sec.id, {
      title: formVal.title,
      content: formVal.content
    }).pipe(
      finalize(() => this.isSaving.set(false))
    ).subscribe({
      next: (updated) => {
        this.sections.update(secs => secs.map(s => s.id === updated.id ? updated : s));
        this.selectedSection.set(updated);
        this.showSuccess('Section saved successfully');
      },
      error: () => this.showError('Failed to save section')
    });
  }

  addSection() {
    this.isSaving.set(true);
    const newOrderIndex = this.sections().length;
    this.docService.addSection(this.docId(), {
      title: 'New Section',
      content: '',
      orderIndex: newOrderIndex
    }).pipe(
      finalize(() => this.isSaving.set(false))
    ).subscribe({
      next: (newSec) => {
        this.sections.update(secs => [...secs, newSec]);
        this.selectSection(newSec);
        this.showSuccess('Section added');
      },
      error: () => this.showError('Failed to add section')
    });
  }

  deleteSection(sectionId: string, event: Event) {
    event.stopPropagation();
    if (!confirm('Are you sure you want to delete this section?')) return;

    this.docService.deleteSection(this.docId(), sectionId).subscribe({
      next: () => {
        this.sections.update(secs => secs.filter(s => s.id !== sectionId));
        if (this.selectedSection()?.id === sectionId) {
          this.selectedSection.set(null);
          this.editorForm.reset();
          if (this.sections().length > 0) {
            this.selectSection(this.sections()[0]);
          }
        }
        this.showSuccess('Section deleted');
      },
      error: () => this.showError('Failed to delete section')
    });
  }

  drop(event: CdkDragDrop<DocSection[]>) {
    const secs = [...this.sections()];
    moveItemInArray(secs, event.previousIndex, event.currentIndex);
    
    // update orderIndex
    const updatedSecs = secs.map((s, idx) => ({ ...s, orderIndex: idx }));
    this.sections.set(updatedSecs);

    this.docService.reorderSections(this.docId(), {
      sectionIds: updatedSecs.map(s => s.id)
    }).subscribe({
      error: () => {
        this.showError('Failed to reorder sections');
        this.loadSections(this.docId()); // reload to revert
      }
    });
  }

  addComment() {
    if (this.commentForm.invalid || !this.selectedSection()) return;

    const content = this.commentForm.getRawValue().content;
    this.docService.addComment(this.docId(), this.selectedSection()!.id, { content }).subscribe({
      next: (newComment) => {
        this.comments.update(c => [...c, newComment]);
        this.commentForm.reset();
        this.showSuccess('Comment added');
      },
      error: () => this.showError('Failed to add comment')
    });
  }

  exportDoc(format: string) {
    // Normalize format for backend
    const normalizedFormat = this.normalizeFormat(format);
    const extension = this.getFileExtension(normalizedFormat);
    
    this.showSuccess(`Exporting document as ${format.toUpperCase()}...`);
    
    // Using HttpClient to pass the Authorization header via interceptor
    this.docService.exportDoc(this.docId(), normalizedFormat).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.doc()?.title || 'document'}.${extension}`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.showSuccess('Document exported successfully');
      },
      error: (err) => {
        console.error('Export error:', err);
        this.showError('Failed to export document');
      }
    });
  }

  private normalizeFormat(format: string): string {
    // Normalize 'markdown' to 'md' for backend
    if (format === 'markdown') {
      return 'md';
    }
    return format.toLowerCase();
  }

  private getFileExtension(format: string): string {
    switch (format.toLowerCase()) {
      case 'pdf':
        return 'pdf';
      case 'html':
        return 'html';
      case 'md':
      case 'markdown':
        return 'md';
      default:
        return 'md';
    }
  }

  private showSuccess(msg: string) {
    this.snackBar.open(msg, 'Close', { duration: 3000 });
  }

  private showError(msg: string) {
    this.snackBar.open(msg, 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
  }

  goBack() {
    this.router.navigate(['/projects']);
  }
}
