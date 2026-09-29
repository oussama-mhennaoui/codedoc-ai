import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateProjectRequest, Project, UpdateProjectRequest } from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/projects`;

  list(): Observable<Project[]> {
    return this.http.get<Project[]>(this.apiUrl);
  }

  create(request: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, request);
  }

  get(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/${id}`);
  }

  update(id: number, request: UpdateProjectRequest): Observable<Project> {
    return this.http.patch<Project>(`${this.apiUrl}/${id}`, request);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  archive(id: number): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/${id}/archive`, {});
  }
}
