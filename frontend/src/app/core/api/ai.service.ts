import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GenerateDocRequest {
  fileId: number;
}

export interface DetectComplexityRequest {
  fileId: number;
}

export interface GenerateDocResponse {
  docId: string;
}

export interface DetectComplexityResponse {
  complexityScore: string;
  details: string;
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

  detectComplexity(request: DetectComplexityRequest): Observable<DetectComplexityResponse> {
    return this.http.post<DetectComplexityResponse>(`${this.apiUrl}/detect-complexity`, request);
  }
}
