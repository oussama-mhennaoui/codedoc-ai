import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChatRequest,
  ChatResponse,
  ChatConversation,
  ChatMessage
} from '../models/chat.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/chat`;

  /** Envoie une question au chatbot RAG */
  sendMessage(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.apiUrl, request);
  }

  /** Récupère toutes les conversations d'un projet */
  getConversations(projectId: number): Observable<ChatConversation[]> {
    const params = new HttpParams().set('projectId', projectId.toString());
    return this.http.get<ChatConversation[]>(`${this.apiUrl}/conversations`, { params });
  }

  /** Récupère l'historique des messages d'une conversation */
  getMessages(conversationId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/conversations/${conversationId}/messages`);
  }

  /** Supprime une conversation */
  deleteConversation(conversationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/conversations/${conversationId}`);
  }

  /** Récupère les questions suggérées */
  getSuggestedQuestions(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/suggested-questions`);
  }
}
