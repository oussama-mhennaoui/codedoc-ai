import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateSourceFileRequest, SourceFile, SourceFileDetail, UpdateSourceFileRequest } from '../models/source-file.model';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api`;

  listForProject(projectId: number): Observable<SourceFile[]> {
    return this.http.get<SourceFile[]>(`${this.apiUrl}/projects/${projectId}/files`);
  }

  create(projectId: number, request: CreateSourceFileRequest): Observable<SourceFileDetail> {
    return this.http.post<SourceFileDetail>(`${this.apiUrl}/projects/${projectId}/files`, request);
  }

  get(id: number): Observable<SourceFileDetail> {
    return this.http.get<SourceFileDetail>(`${this.apiUrl}/files/${id}`);
  }

  update(id: number, request: UpdateSourceFileRequest): Observable<SourceFileDetail> {
    return this.http.patch<SourceFileDetail>(`${this.apiUrl}/files/${id}`, request);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/files/${id}`);
  }
}
