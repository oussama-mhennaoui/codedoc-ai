import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export enum Severity {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
  INFO = 'INFO'
}

export interface Vulnerability {
  id: number;
  type: string;
  severity: Severity;
  fileName: string;
  line: number;
  description: string;
  consequences: string;
  fix: string;
  cweId: string;
  correctedCode: string;
}

export interface SecurityAudit {
  id: number;
  projectId: number;
  sourceFileId: number;
  fileName: string;
  status: string;
  securityScore: string;
  summary: string;
  createdAt: string;
  vulnerabilities: Vulnerability[];
}

export interface SecurityAuditRequest {
  projectId?: number;
  fileId: number;
}

@Injectable({
  providedIn: 'root'
})
export class SecurityService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/audit`;

  performAudit(request: SecurityAuditRequest): Observable<SecurityAudit> {
    return this.http.post<SecurityAudit>(`${this.apiUrl}`, request);
  }

  getProjectAudits(projectId: number): Observable<SecurityAudit[]> {
    return this.http.get<SecurityAudit[]>(`${this.apiUrl}/project/${projectId}`);
  }

  getFileAudits(fileId: number): Observable<SecurityAudit[]> {
    return this.http.get<SecurityAudit[]>(`${this.apiUrl}/file/${fileId}`);
  }

  getAudit(auditId: number): Observable<SecurityAudit> {
    return this.http.get<SecurityAudit>(`${this.apiUrl}/${auditId}`);
  }

  exportPdf(auditId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${auditId}/export/pdf`, { responseType: 'blob' });
  }
}
