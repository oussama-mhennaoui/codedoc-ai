export interface GeneratedDoc {
  id: string;
  projectId: string;
  title: string;
  sourceFileId: string;
  sourceFileName: string;
  sourceFileLanguage?: string;
  sourceFileSizeBytes?: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocSection {
  id: string;
  docId: string;
  title: string;
  content: string;
  orderIndex: number;
  type?: string;
  createdAt: string;
}

export interface Comment {
  id: string;
  sectionId: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
}

export interface UpdateDocRequest {
  title?: string;
  status?: 'PENDING' | 'SUCCESS' | 'FAILED';
  errorMessage?: string;
}

export interface AddSectionRequest {
  title: string;
  content: string;
  orderIndex: number;
  type?: string;
}

export interface UpdateSectionRequest {
  title?: string;
  content?: string;
  orderIndex?: number;
  type?: string;
}

export interface AddCommentRequest {
  content: string;
}

export interface ReorderSectionsRequest {
  sectionIds: string[];
}
