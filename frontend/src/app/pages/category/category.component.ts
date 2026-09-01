import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, switchMap } from 'rxjs';

import { toErrorMessage } from '../../core/error-message';
import { CategoryService } from '../../core/services/category.service';
import { ProgressService } from '../../core/services/progress.service';
import { CategoryDetail } from '../../models/category';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { LoadingComponent } from '../../shared/loading/loading.component';

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [RouterLink, EmptyStateComponent, LoadingComponent],
  templateUrl: './category.component.html',
  styleUrl: './category.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryComponent {
  private readonly categoryService = inject(CategoryService);
  private readonly progressService = inject(ProgressService);
  private readonly titleService = inject(Title);

  /** Bound from the :slug route parameter by withComponentInputBinding. */
  readonly slug = input.required<string>();

  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly category = signal<CategoryDetail | null>(null);

  readonly completedCount = computed(() => {
    this.progressService.completedIds();
    const articles = this.category()?.articles ?? [];
    return this.progressService.countCompleted(articles.map((article) => article.id));
  });

  readonly progressPercent = computed(() => {
    const total = this.category()?.articles.length ?? 0;
    return total === 0 ? 0 : Math.round((this.completedCount() / total) * 100);
  });

  constructor() {
    // Reloads whenever the route slug changes, and keeps listening after a failed request.
    toObservable(this.slug)
      .pipe(
        switchMap((slug) => {
          this.loading.set(true);
          this.errorMessage.set('');
          this.category.set(null);
          return this.categoryService.findBySlug(slug).pipe(
            catchError((error: unknown) => {
              this.errorMessage.set(toErrorMessage(error, 'This category could not be loaded.'));
              this.loading.set(false);
              return EMPTY;
            })
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe((category) => {
        this.category.set(category);
        this.titleService.setTitle(`${category.name} | Technical Blog`);
        this.loading.set(false);
      });
  }

  isCompleted(articleId: number): boolean {
    this.progressService.completedIds();
    return this.progressService.isCompleted(articleId);
  }

  toggleCompleted(articleId: number): void {
    this.progressService.toggle(articleId);
  }
}
