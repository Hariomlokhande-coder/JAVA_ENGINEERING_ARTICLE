import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'technical-blog.theme';

/** Keeps the dark/light choice on the html element and in local storage. */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly current = signal<Theme>('light');
  readonly theme = this.current.asReadonly();

  init(): void {
    // The light finish is the default look; dark is opt in through the toggle.
    this.apply(this.readStoredTheme() ?? 'light');
  }

  toggle(): void {
    this.apply(this.current() === 'dark' ? 'light' : 'dark');
  }

  private apply(theme: Theme): void {
    this.current.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      // Theme simply resets on the next visit when storage is unavailable.
    }
  }

  private readStoredTheme(): Theme | null {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored === 'dark' || stored === 'light' ? stored : null;
    } catch {
      return null;
    }
  }

}
