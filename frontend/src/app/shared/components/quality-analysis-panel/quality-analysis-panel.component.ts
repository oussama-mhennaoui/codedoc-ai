import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CodeSmell, QualityReport } from '../../../core/models/quality.model';

@Component({
  selector: 'app-quality-analysis-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule,
    MatMenuModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './quality-analysis-panel.component.html',
  styleUrl: './quality-analysis-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QualityAnalysisPanelComponent {
  report = input<QualityReport | null>(null);
  isLoading = input<boolean>(false);
  filename = input<string>('');

  analyze = output<void>();
  exportPdf = output<void>();
  exportMarkdown = output<void>();
  selectLine = output<number>();

  filterSeverity = signal<'all' | 'critique' | 'moyen' | 'faible'>('all');

  filteredSmells = computed(() => {
    const rep = this.report();
    if (!rep || !rep.codeSmells) return [];
    const filter = this.filterSeverity();
    if (filter === 'all') return rep.codeSmells;
    return rep.codeSmells.filter(s => s.gravite?.toLowerCase() === filter);
  });

  criticalCount = computed(() => {
    return this.report()?.codeSmells?.filter(s => s.gravite?.toLowerCase() === 'critique').length ?? 0;
  });

  mediumCount = computed(() => {
    return this.report()?.codeSmells?.filter(s => s.gravite?.toLowerCase() === 'moyen').length ?? 0;
  });

  lowCount = computed(() => {
    return this.report()?.codeSmells?.filter(s => s.gravite?.toLowerCase() === 'faible').length ?? 0;
  });

  scoreColorClass = computed(() => {
    const score = this.report()?.scoreGlobal ?? 0;
    if (score >= 80) return 'score-good';
    if (score >= 50) return 'score-medium';
    return 'score-poor';
  });

  scoreLabel = computed(() => {
    const score = this.report()?.scoreGlobal ?? 0;
    if (score >= 85) return 'Excellente qualité';
    if (score >= 70) return 'Bonne qualité';
    if (score >= 50) return 'Qualité moyenne';
    return 'Qualité critique';
  });

  debtBadgeClass = computed(() => {
    const rating = this.report()?.technicalDebtRating?.toLowerCase() ?? 'faible';
    return `debt-${rating}`;
  });

  setFilter(filter: 'all' | 'critique' | 'moyen' | 'faible') {
    this.filterSeverity.set(filter);
  }

  onLineClick(line: number) {
    if (line > 0) {
      this.selectLine.emit(line);
    }
  }
}
