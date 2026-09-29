export interface Project {
  id: number;
  name: string;
  description: string;
  language: string;
  createdAt: string;
  updatedAt: string;
  archived: boolean;
  fileCount: number;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
  language?: string;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
  language?: string;
  archived?: boolean;
}
