import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HighlightModule } from 'ngx-highlightjs';
import { HighlightLineNumbers } from 'ngx-highlightjs/line-numbers';

@Component({
  selector: 'app-code-viewer',
  standalone: true,
  imports: [CommonModule, HighlightModule, HighlightLineNumbers],
  templateUrl: './code-viewer.component.html',
  styleUrl: './code-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CodeViewerComponent {
  @Input({ required: true }) code: string = '';
  @Input() language: string = '';
}
