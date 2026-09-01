import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { toErrorMessage } from '../../../core/error-message';
import { ArticleService } from '../../../core/services/article.service';
import { CategoryService } from '../../../core/services/category.service';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ArticleSummary } from '../../../models/article';
import { EmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { LoadingComponent } from '../../../shared/loading/loading.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, FormsModule, EmptyStateComponent, LoadingComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly categoryService = inject(CategoryService);
  private readonly confirmService = inject(ConfirmService);

  readonly articles = signal<ArticleSummary[]>([]);
  readonly totalArticles = signal(0);
  readonly totalCategories = signal(0);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly keyword = signal('');
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly deletingId = signal<number | null>(null);
  readonly togglingId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    forkJoin({
      articles: this.articleService.findForAdmin(this.keyword(), this.page()),
      categories: this.categoryService.findAll()
    }).subscribe({
      next: ({ articles, categories }) => {
        this.articles.set(articles.content);
        this.totalArticles.set(articles.totalElements);
        this.totalPages.set(articles.totalPages);
        this.page.set(articles.page);
        this.totalCategories.set(categories.length);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(toErrorMessage(error, 'The dashboard could not be loaded.'));
        this.loading.set(false);
      }
    });
  }

  applySearch(): void {
    this.page.set(0);
    this.loadAll();
  }

  clearSearch(): void {
    this.keyword.set('');
    this.applySearch();
  }

  goToPage(page: number): void {
    if (page < 0 || (this.totalPages() > 0 && page >= this.totalPages())) {
      return;
    }
    this.page.set(page);
    this.loadAll();
  }

  togglePublished(article: ArticleSummary): void {
    this.togglingId.set(article.id);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.articleService.setPublished(article.id, !article.published).subscribe({
      next: (updated) => {
        this.togglingId.set(null);
        this.successMessage.set(`"${updated.title}" is now ${updated.published ? 'published' : 'a draft'}.`);
        this.articles.update((list) =>
          list.map((item) => (item.id === updated.id ? { ...item, published: updated.published } : item))
        );
      },
      error: (error: unknown) => {
        this.togglingId.set(null);
        this.errorMessage.set(toErrorMessage(error, 'The article status could not be changed.'));
      }
    });
  }

  async remove(article: ArticleSummary): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: `Delete "${article.title}"?`,
      message: 'The article and its tag links are removed permanently. This cannot be undone.',
      confirmLabel: 'Delete article',
      danger: true
    });

    if (!confirmed) {
      return;
    }

    this.deletingId.set(article.id);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.articleService.delete(article.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.successMessage.set(`"${article.title}" was deleted.`);
        // Step back a page when the last row of the current page is gone.
        if (this.articles().length === 1 && this.page() > 0) {
          this.page.update((current) => current - 1);
        }
        this.loadAll();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.errorMessage.set(toErrorMessage(error, 'The article could not be deleted.'));
      }
    });
  }
}
