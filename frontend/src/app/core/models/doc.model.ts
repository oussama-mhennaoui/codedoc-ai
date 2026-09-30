export interface GeneratedDoc {
  id: string;
  projectId: string;
  title: string;
  sourceFileId?: string;
  sourceFileName?: string;
  sourceFileLanguage?: string;
  sourceFileSizeBytes?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocSection {
  id: string;
  docId: string;
  title: string;
  content: string;
  orderIndex: number;
}

export interface Comment {
  id: string;
  sectionId: string;
  authorId: string;
  authorName?: string;
  content: string;
  createdAt: string;
}

export interface CreateDocRequest {
  projectId: string;
  title: string;
}

export interface UpdateDocRequest {
  title: string;
}

export interface AddSectionRequest {
  title: string;
  content: string;
  orderIndex: number;
}

export interface UpdateSectionRequest {
  title?: string;
  content?: string;
  orderIndex?: number;
}

export interface AddCommentRequest {
  content: string;
}

export interface ReorderSectionsRequest {
  sectionIds: string[];
}
