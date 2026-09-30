import { Injectable, effect, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';

type ThemeMode = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private document = inject(DOCUMENT);
  
  private readonly _mode = signal<ThemeMode>('light');
  readonly mode = this._mode.asReadonly();

  constructor() {
    this.init();
    
    // Watch for theme changes and apply to DOM
    effect(() => {
      const theme = this._mode();
      this.applyTheme(theme);
    });
  }

  /**
   * Initialize theme from localStorage or system preference
   */
  private init(): void {
    const savedTheme = localStorage.getItem('theme') as ThemeMode | null;
    
    if (savedTheme === 'light' || savedTheme === 'dark') {
      this._mode.set(savedTheme);
    } else {
      // Check system preference
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      this._mode.set(prefersDark ? 'dark' : 'light');
    }
  }

  /**
   * Toggle between light and dark mode
   */
  toggle(): void {
    this._mode.set(this.mode() === 'light' ? 'dark' : 'light');
    this.persistTheme();
  }

  /**
   * Set theme to specific mode
   */
  setMode(mode: ThemeMode): void {
    this._mode.set(mode);
    this.persistTheme();
  }

  /**
   * Get current theme mode
   */
  getMode(): ThemeMode {
    return this.mode();
  }

  /**
   * Apply theme to document
   */
  private applyTheme(theme: ThemeMode): void {
    const htmlElement = this.document.documentElement;
    
    if (theme === 'dark') {
      htmlElement.setAttribute('data-theme', 'dark');
      htmlElement.classList.add('dark-theme');
      htmlElement.classList.remove('light-theme');
    } else {
      htmlElement.removeAttribute('data-theme');
      htmlElement.classList.add('light-theme');
      htmlElement.classList.remove('dark-theme');
    }
  }

  /**
   * Persist theme to localStorage
   */
  private persistTheme(): void {
    localStorage.setItem('theme', this.mode());
  }
}
