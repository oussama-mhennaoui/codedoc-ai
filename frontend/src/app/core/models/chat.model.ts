export interface SourceCitation {
  filename: string;
  startLine: number;
  endLine: number;
  snippet: string;
}

export interface ChatMessage {
  id?: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  sources: SourceCitation[];
  createdAt?: string;
}

export interface ChatConversation {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

export interface ChatRequest {
  projectId: number;
  conversationId?: number | null;
  question: string;
}

export interface ChatResponse {
  conversationId: number;
  answer: string;
  sources: SourceCitation[];
  createdAt: string;
}
