import { Injectable, effect, inject, signal } from '@angular/core';

import { AuthService } from './auth.service';
import { ProgressApiService } from './progress-api.service';

const STORAGE_KEY = 'technical-blog.progress';
const FAVOURITE_KEY = 'technical-blog.favorites';

/**
 * Reading progress and favourites.
 * A visitor without an account keeps them in this browser. Once signed in the
 * server becomes the source of truth, and anything tracked before signing in is
 * pushed up once so nothing is lost.
 */
@Injectable({ providedIn: 'root' })
export class ProgressService {
  private readonly authService = inject(AuthService);
  private readonly api = inject(ProgressApiService);

  private readonly completed = signal<ReadonlySet<number>>(this.readStored(STORAGE_KEY));
  private readonly favourites = signal<ReadonlySet<number>>(this.readStored(FAVOURITE_KEY));

  readonly completedIds = this.completed.asReadonly();
  readonly favouriteIds = this.favourites.asReadonly();

  constructor() {
    // Follows sign in and sign out without the components having to care.
    effect(() => {
      if (this.authService.isLoggedIn()) {
        this.pullFromServer();
      }
    });
  }

  isCompleted(articleId: number): boolean {
    return this.completed().has(articleId);
  }

  isFavourite(articleId: number): boolean {
    return this.favourites().has(articleId);
  }

  countCompleted(articleIds: number[]): number {
    const current = this.completed();
    return articleIds.reduce((total, id) => (current.has(id) ? total + 1 : total), 0);
  }

  toggle(articleId: number): void {
    const next = this.flip(this.completed(), articleId);
    this.completed.set(next);
    this.persist(STORAGE_KEY, next);
    this.push(articleId, { completed: next.has(articleId) });
  }

  toggleFavourite(articleId: number): void {
    const next = this.flip(this.favourites(), articleId);
    this.favourites.set(next);
    this.persist(FAVOURITE_KEY, next);
    this.push(articleId, { favourite: next.has(articleId) });
  }

  private flip(current: ReadonlySet<number>, articleId: number): Set<number> {
    const next = new Set(current);
    if (!next.delete(articleId)) {
      next.add(articleId);
    }
    return next;
  }

  /** Saves one change for signed in readers; anonymous ones stay local. */
  private push(articleId: number, change: { completed?: boolean; favourite?: boolean }): void {
    if (!this.authService.isLoggedIn()) {
      return;
    }
    this.api.save(articleId, change).subscribe({
      error: () => {
        // The local copy already reflects the change, so a failed sync is not fatal.
      }
    });
  }

  /** Merges whatever this browser tracked into the account, then adopts the server state. */
  private pullFromServer(): void {
    this.api.findMine().subscribe({
      next: (entries) => {
        const serverCompleted = new Set(entries.filter((e) => e.completed).map((e) => e.articleId));
        const serverFavourites = new Set(entries.filter((e) => e.favourite).map((e) => e.articleId));

        this.completed().forEach((id) => {
          if (!serverCompleted.has(id)) {
            serverCompleted.add(id);
            this.api.save(id, { completed: true }).subscribe({ error: () => undefined });
          }
        });
        this.favourites().forEach((id) => {
          if (!serverFavourites.has(id)) {
            serverFavourites.add(id);
            this.api.save(id, { favourite: true }).subscribe({ error: () => undefined });
          }
        });

        this.completed.set(serverCompleted);
        this.favourites.set(serverFavourites);
      },
      error: () => {
        // Offline or expired session: the local copy keeps working.
      }
    });
  }

  private readStored(key: string): ReadonlySet<number> {
    try {
      const raw = localStorage.getItem(key);
      const parsed: unknown = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed)
        ? new Set(parsed.filter((value): value is number => typeof value === 'number'))
        : new Set<number>();
    } catch {
      return new Set<number>();
    }
  }

  private persist(key: string, ids: ReadonlySet<number>): void {
    try {
      localStorage.setItem(key, JSON.stringify([...ids]));
    } catch {
      // Progress is a convenience, losing it must never break the page.
    }
  }
}
