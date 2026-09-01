import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, catchError, map, switchMap } from 'rxjs';

import { toErrorMessage } from '../../core/error-message';
import { ArticleService } from '../../core/services/article.service';
import { ArticleSummary } from '../../models/article';
import { ArticleCardComponent } from '../../shared/article-card/article-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { LoadingComponent } from '../../shared/loading/loading.component';

const MIN_KEYWORD_LENGTH = 2;

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [FormsModule, ArticleCardComponent, EmptyStateComponent, LoadingComponent],
  templateUrl: './search.component.html',
  styleUrl: './search.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly articleService = inject(ArticleService);
  private readonly titleService = inject(Title);

  readonly keyword = signal('');
  readonly results = signal<ArticleSummary[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly searched = signal(false);
  /** Bound to the search box on this page, separate from the applied keyword. */
  readonly draftKeyword = signal('');

  constructor() {
    this.route.queryParamMap
      .pipe(
        map((params) => (params.get('q') ?? '').trim()),
        switchMap((keyword) => {
          this.keyword.set(keyword);
          this.draftKeyword.set(keyword);
          this.titleService.setTitle(keyword ? `${keyword} | Search` : 'Search | Technical Blog');
          this.errorMessage.set('');

          if (keyword.length < MIN_KEYWORD_LENGTH) {
            this.results.set([]);
            this.total.set(0);
            this.totalPages.set(0);
            this.searched.set(keyword.length > 0);
            this.loading.set(false);
            return EMPTY;
          }

          // A new keyword always starts from the first page of results.
          this.page.set(0);
          this.loading.set(true);
          this.searched.set(true);
          return this.articleService.search(keyword, 0).pipe(
            catchError((error: unknown) => {
              this.errorMessage.set(toErrorMessage(error, 'The search could not be completed.'));
              this.loading.set(false);
              return EMPTY;
            })
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe((result) => {
        this.results.set(result.content);
        this.total.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.page.set(result.page);
        this.loading.set(false);
      });
  }

  goToPage(page: number): void {
    if (page < 0 || (this.totalPages() > 0 && page >= this.totalPages())) {
      return;
    }
    this.loading.set(true);
    this.articleService.search(this.keyword(), page).subscribe({
      next: (result) => {
        this.results.set(result.content);
        this.total.set(result.totalElements);
        this.totalPages.set(result.totalPages);
        this.page.set(result.page);
        this.loading.set(false);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (error: unknown) => {
        this.errorMessage.set(toErrorMessage(error, 'The search could not be completed.'));
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    const keyword = this.draftKeyword().trim();
    if (keyword.length < MIN_KEYWORD_LENGTH) {
      return;
    }
    void this.router.navigate(['/search'], { queryParams: { q: keyword } });
  }

  clear(): void {
    this.draftKeyword.set('');
    void this.router.navigate(['/search']);
  }
}
