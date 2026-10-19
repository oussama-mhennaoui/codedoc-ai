import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GenerateTestsRequest, TestGeneration } from '../models/test-generation.model';

@Injectable({
  providedIn: 'root'
})
export class TestGenerationService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/test-generations`;

  generate(request: GenerateTestsRequest): Observable<TestGeneration> {
    return this.http.post<TestGeneration>(this.apiUrl, request);
  }

  getById(id: number): Observable<TestGeneration> {
    return this.http.get<TestGeneration>(`${this.apiUrl}/${id}`);
  }

  getHistory(): Observable<TestGeneration[]> {
    return this.http.get<TestGeneration[]>(`${this.apiUrl}/history`);
  }

  getByProject(projectId: number): Observable<TestGeneration[]> {
    return this.http.get<TestGeneration[]>(`${this.apiUrl}/by-project/${projectId}`);
  }

  downloadUrl(id: number): string {
    return `${this.apiUrl}/${id}/download`;
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, {
      responseType: 'blob'
    });
  }
}
