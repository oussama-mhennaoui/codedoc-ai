import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { QualityReport, ProjectQualitySummary } from '../models/quality.model';

@Injectable({
  providedIn: 'root'
})
export class QualityService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/quality`;

  analyze(fileId: number): Observable<QualityReport> {
    return this.http.post<QualityReport>(`${this.apiUrl}/analyze`, { fileId });
  }

  getReportByFile(fileId: number): Observable<QualityReport> {
    return this.http.get<QualityReport>(`${this.apiUrl}/files/${fileId}`);
  }

  getProjectSummary(projectId: number): Observable<ProjectQualitySummary> {
    return this.http.get<ProjectQualitySummary>(`${this.apiUrl}/projects/${projectId}/summary`);
  }

  exportReport(reportId: number, format: 'pdf' | 'md' = 'pdf'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/${reportId}/export?format=${format}`, {
      responseType: 'blob'
    });
  }

  downloadReport(reportId: number, filename: string, format: 'pdf' | 'md' = 'pdf'): void {
    this.exportReport(reportId, format).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        const ext = format === 'pdf' ? 'pdf' : 'md';
        const cleanName = filename.replace(/\.[^/.]+$/, '');
        a.href = url;
        a.download = `quality-report-${cleanName}.${ext}`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      }
    });
  }
}
