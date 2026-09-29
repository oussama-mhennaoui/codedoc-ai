import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  GeneratedDoc,
  DocSection,
  Comment,
  UpdateDocRequest,
  AddSectionRequest,
  UpdateSectionRequest,
  AddCommentRequest,
  ReorderSectionsRequest
} from '../models/doc.model';

@Injectable({
  providedIn: 'root'
})
export class DocService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/docs`;

  getDoc(id: string): Observable<GeneratedDoc> {
    return this.http.get<GeneratedDoc>(`${this.apiUrl}/${id}`).pipe(
      tap(() => console.log(`Fetched doc ${id}`)),
      catchError(this.handleError)
    );
  }

  listByProject(projectId: string): Observable<GeneratedDoc[]> {
    return this.http.get<GeneratedDoc[]>(`${environment.apiUrl}/api/projects/${projectId}/docs`).pipe(
      tap(docs => console.log(`Fetched ${docs.length} docs for project ${projectId}`)),
      catchError(this.handleError)
    );
  }

  updateDoc(id: string, request: UpdateDocRequest): Observable<GeneratedDoc> {
    return this.http.put<GeneratedDoc>(`${this.apiUrl}/${id}`, request).pipe(
      tap(() => console.log(`Updated doc ${id}`)),
      catchError(this.handleError)
    );
  }

  deleteDoc(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => console.log(`Deleted doc ${id}`)),
      catchError(this.handleError)
    );
  }

  listSections(docId: string): Observable<DocSection[]> {
    return this.http.get<DocSection[]>(`${this.apiUrl}/${docId}/sections`).pipe(
      tap(sections => console.log(`Fetched ${sections.length} sections for doc ${docId}`)),
      catchError(this.handleError)
    );
  }

  addSection(docId: string, request: AddSectionRequest): Observable<DocSection> {
    return this.http.post<DocSection>(`${this.apiUrl}/${docId}/sections`, request).pipe(
      tap(section => console.log(`Added section ${section.id} to doc ${docId}`)),
      catchError(this.handleError)
    );
  }

  updateSection(docId: string, sectionId: string, request: UpdateSectionRequest): Observable<DocSection> {
    return this.http.patch<DocSection>(`${environment.apiUrl}/api/sections/${sectionId}`, request).pipe(
      tap(() => console.log(`Updated section ${sectionId} in doc ${docId}`)),
      catchError(this.handleError)
    );
  }

  deleteSection(docId: string, sectionId: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/sections/${sectionId}`).pipe(
      tap(() => console.log(`Deleted section ${sectionId} from doc ${docId}`)),
      catchError(this.handleError)
    );
  }

  reorderSections(docId: string, request: ReorderSectionsRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${docId}/sections/reorder`, request.sectionIds).pipe(
      tap(() => console.log(`Reordered sections in doc ${docId}`)),
      catchError(this.handleError)
    );
  }

  listComments(docId: string, sectionId: string): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${environment.apiUrl}/api/sections/${sectionId}/comments`).pipe(
      tap(comments => console.log(`Fetched ${comments.length} comments for section ${sectionId}`)),
      catchError(this.handleError)
    );
  }

  addComment(docId: string, sectionId: string, request: AddCommentRequest): Observable<Comment> {
    return this.http.post<Comment>(`${environment.apiUrl}/api/sections/${sectionId}/comments`, request).pipe(
      tap(comment => console.log(`Added comment ${comment.id} to section ${sectionId}`)),
      catchError(this.handleError)
    );
  }

  exportDoc(docId: string, format: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${docId}/export?format=${format}`, {
      responseType: 'blob'
    }).pipe(
      tap(() => console.log(`Exported doc ${docId} as ${format}`)),
      catchError(this.handleError)
    );
  }

  private handleError(error: any) {
    console.error('API Error:', error);
    return throwError(() => new Error(error.message || 'Server error'));
  }
}
