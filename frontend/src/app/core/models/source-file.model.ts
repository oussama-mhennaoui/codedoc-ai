export interface SourceFile {
  id: number;
  filename: string;
  language: string;
  sizeBytes: number;
  checksum: string;
  uploadedAt: string;
}

export interface SourceFileDetail extends SourceFile {
  content: string;
  projectId: number;
}

export interface CreateSourceFileRequest {
  filename: string;
  content: string;
  language?: string;
}

export interface UpdateSourceFileRequest {
  filename?: string;
  content?: string;
  language?: string;
}
