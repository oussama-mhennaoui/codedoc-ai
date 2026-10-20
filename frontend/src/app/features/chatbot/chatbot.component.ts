import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Input,
  OnInit,
  signal,
  ViewChild,
  AfterViewChecked
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ChatService } from '../../core/api/chat.service';
import { ChatMessage, ChatConversation, SourceCitation } from '../../core/models/chat.model';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChatbotComponent implements OnInit, AfterViewChecked {
  @Input({ required: true }) projectId!: number;
  @Input() initialQuestion: string | null = null; // Pour "Expliquer cette fonction"

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  private chatService = inject(ChatService);
  private snackBar = inject(MatSnackBar);

  // ─── State ────────────────────────────────────────────────────────────────
  isOpen = signal(false);
  isLoading = signal(false);
  showHistory = signal(false);

  messages = signal<ChatMessage[]>([]);
  conversations = signal<ChatConversation[]>([]);
  suggestedQuestions = signal<string[]>([]);

  currentConversationId = signal<number | null>(null);
  userInput = signal('');
  private shouldScroll = false;

  ngOnInit() {
    this.chatService.getSuggestedQuestions().subscribe({
      next: (q) => this.suggestedQuestions.set(q),
      error: () => {}
    });

    // Si une question initiale est fournie (bouton "Expliquer")
    if (this.initialQuestion) {
      this.isOpen.set(true);
      this.sendQuestion(this.initialQuestion);
    }
  }

  ngAfterViewChecked() {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  // ─── Ouvre / Ferme le panel ────────────────────────────────────────────────
  toggleChat() {
    this.isOpen.update(v => !v);
    if (this.isOpen() && this.messages().length === 0) {
      this.newConversation();
    }
  }

  // ─── Nouvelle conversation ─────────────────────────────────────────────────
  newConversation() {
    this.messages.set([]);
    this.currentConversationId.set(null);
    this.showHistory.set(false);
  }

  // ─── Historique ───────────────────────────────────────────────────────────
  openHistory() {
    this.showHistory.set(true);
    this.chatService.getConversations(this.projectId).subscribe({
      next: (convs) => this.conversations.set(convs),
      error: () => this.snackBar.open('Erreur lors du chargement de l\'historique', 'Fermer', { duration: 3000 })
    });
  }

  loadConversation(conv: ChatConversation) {
    this.currentConversationId.set(conv.id);
    this.showHistory.set(false);
    this.chatService.getMessages(conv.id).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
        this.shouldScroll = true;
      }
    });
  }

  deleteConversation(event: Event, convId: number) {
    event.stopPropagation();
    this.chatService.deleteConversation(convId).subscribe({
      next: () => {
        this.conversations.update(c => c.filter(x => x.id !== convId));
        if (this.currentConversationId() === convId) this.newConversation();
      }
    });
  }

  // ─── Envoi message ────────────────────────────────────────────────────────
  sendMessage() {
    const question = this.userInput().trim();
    if (!question || this.isLoading()) return;
    this.sendQuestion(question);
    this.userInput.set('');
  }

  sendQuestion(question: string) {
    // Ajoute le message utilisateur immédiatement
    const userMsg: ChatMessage = { role: 'USER', content: question, sources: [] };
    this.messages.update(msgs => [...msgs, userMsg]);
    this.shouldScroll = true;
    this.isLoading.set(true);

    this.chatService.sendMessage({
      projectId: this.projectId,
      conversationId: this.currentConversationId(),
      question
    }).subscribe({
      next: (resp) => {
        this.currentConversationId.set(resp.conversationId);
        const assistantMsg: ChatMessage = {
          role: 'ASSISTANT',
          content: resp.answer,
          sources: resp.sources,
          createdAt: resp.createdAt
        };
        this.messages.update(msgs => [...msgs, assistantMsg]);
        this.isLoading.set(false);
        this.shouldScroll = true;
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open('Erreur lors de la communication avec le chatbot', 'Fermer', { duration: 4000 });
      }
    });
  }

  useSuggestion(q: string) {
    this.userInput.set(q);
    this.sendMessage();
  }

  onKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom() {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {}
  }

  // ─── Affichage sources ────────────────────────────────────────────────────
  expandedSources = signal<Set<number>>(new Set());

  toggleSources(index: number) {
    this.expandedSources.update(set => {
      const newSet = new Set(set);
      newSet.has(index) ? newSet.delete(index) : newSet.add(index);
      return newSet;
    });
  }

  isSourceExpanded(index: number): boolean {
    return this.expandedSources().has(index);
  }

  formatSource(src: SourceCitation): string {
    return `${src.filename}:L${src.startLine}-${src.endLine}`;
  }
}
