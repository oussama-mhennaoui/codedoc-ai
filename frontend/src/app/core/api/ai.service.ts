import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AiLog, AiMetrics } from '../models/ai-metrics.model';

export interface GenerateDocRequest {
  fileId: number;
}

export interface GenerateDocResponse {
  docId: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/ai`;

  generateDoc(request: GenerateDocRequest): Observable<GenerateDocResponse> {
    return this.http.post<GenerateDocResponse>(`${this.apiUrl}/generate-doc`, request);
  }

  getMetrics(): Observable<AiMetrics> {
    return this.http.get<AiMetrics>(`${this.apiUrl}/metrics`);
  }

  getRecentLogs(): Observable<AiLog[]> {
    return this.http.get<AiLog[]>(`${this.apiUrl}/logs`);
  }
}
