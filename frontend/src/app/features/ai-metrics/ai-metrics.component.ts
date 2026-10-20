import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AiService } from '../../core/api/ai.service';
import { AiLog, AiMetrics, EndpointMetric } from '../../core/models/ai-metrics.model';

@Component({
  selector: 'app-ai-metrics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    DatePipe
  ],
  templateUrl: './ai-metrics.component.html',
  styleUrls: ['./ai-metrics.component.scss']
})
export class AiMetricsComponent implements OnInit {
  private aiService = inject(AiService);

  metrics = signal<AiMetrics | null>(null);
  isLoading = signal<boolean>(true);
  error = signal<string | null>(null);

  displayedColumns: string[] = ['createdAt', 'endpoint', 'username', 'status', 'tokens', 'latencyMs'];

  ngOnInit(): void {
    this.fetchMetrics();
  }

  fetchMetrics(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.aiService.getMetrics().subscribe({
      next: (data) => {
        this.metrics.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load AI metrics:', err);
        this.error.set('Could not fetch AI observability metrics. Please ensure the backend is running.');
        this.isLoading.set(false);
      }
    });
  }

  getEndpointLabel(endpoint: string): string {
    const map: Record<string, string> = {
      'generate_doc': 'Documentation Gen',
      'generate-doc': 'Documentation Gen',
      'security_audit': 'Security OWASP Audit',
      'quality_analysis': 'Quality & SOLID Analysis',
      'test_generation': 'Unit Test Generation',
      'generate_tests': 'Unit Test Generation',
      'chat_rag': 'Contextual RAG Chat',
      'detect_complexity': 'Complexity Analysis',
      'summarize_project': 'Project Summary'
    };
    return map[endpoint] || endpoint;
  }

  getEndpointIcon(endpoint: string): string {
    const map: Record<string, string> = {
      'generate_doc': 'menu_book',
      'generate-doc': 'menu_book',
      'security_audit': 'security',
      'quality_analysis': 'verified',
      'test_generation': 'biotech',
      'generate_tests': 'biotech',
      'chat_rag': 'smart_toy',
      'detect_complexity': 'insights',
      'summarize_project': 'auto_stories'
    };
    return map[endpoint] || 'smart_toy';
  }

  formatTokens(n: number | null | undefined): string {
    if (n == null) return '0';
    if (n >= 1000000) return (n / 1000000).toFixed(2) + 'M';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
    return n.toString();
  }
}
