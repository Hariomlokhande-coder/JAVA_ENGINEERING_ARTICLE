import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { toErrorMessage } from '../../core/error-message';
import { ArticleService } from '../../core/services/article.service';
import { CategoryService } from '../../core/services/category.service';
import { ProgressService } from '../../core/services/progress.service';
import { ArticleSummary } from '../../models/article';
import { CategoryDetail } from '../../models/category';
import { ArticleCardComponent } from '../../shared/article-card/article-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { LoadingComponent } from '../../shared/loading/loading.component';
import { RoadmapSectionComponent } from './roadmap-section/roadmap-section.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ArticleCardComponent, EmptyStateComponent, LoadingComponent, RoadmapSectionComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly articleService = inject(ArticleService);
  private readonly progressService = inject(ProgressService);
  private readonly router = inject(Router);

  readonly sections = signal<CategoryDetail[]>([]);
  readonly latestArticles = signal<ArticleSummary[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  /** Several sections can stay open at once, which is what Expand all relies on. */
  readonly openSectionIds = signal<ReadonlySet<number>>(new Set<number>());

  private readonly allArticleIds = computed(() =>
    this.sections().flatMap((section) => section.articles.map((article) => article.id))
  );

  readonly totalTopics = computed(() => this.allArticleIds().length);

  readonly completedTopics = computed(() => {
    this.progressService.completedIds();
    return this.progressService.countCompleted(this.allArticleIds());
  });

  readonly progressPercent = computed(() => {
    const total = this.totalTopics();
    return total === 0 ? 0 : Math.round((this.completedTopics() / total) * 100);
  });


  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    forkJoin({
      roadmap: this.categoryService.findRoadmap(),
      latest: this.articleService.findPublished(0, 6)
    }).subscribe({
      next: ({ roadmap, latest }) => {
        this.sections.set(roadmap);
        this.latestArticles.set(latest.content);
        const firstWithTopics = roadmap.find((section) => section.articles.length > 0);
        this.openSectionIds.set(new Set(firstWithTopics ? [firstWithTopics.id] : []));
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(toErrorMessage(error, 'The roadmap could not be loaded.'));
        this.loading.set(false);
      }
    });
  }

  /** Opens a random published topic, the shortcut the roadmap screenshots show. */
  openRandomArticle(): void {
    const published = this.sections()
      .flatMap((section) => section.articles)
      .filter((article) => article.published);

    if (published.length === 0) {
      return;
    }
    const pick = published[Math.floor(Math.random() * published.length)];
    void this.router.navigate(['/article', pick.slug]);
  }

  readonly hasPublishedArticles = computed(() =>
    this.sections().some((section) => section.articles.some((article) => article.published))
  );

  isOpen(categoryId: number): boolean {
    return this.openSectionIds().has(categoryId);
  }

  toggleSection(categoryId: number): void {
    this.openSectionIds.update((current) => {
      const next = new Set(current);
      if (!next.delete(categoryId)) {
        next.add(categoryId);
      }
      return next;
    });
  }

  readonly allExpanded = computed(() => {
    const sections = this.sections();
    return sections.length > 0 && sections.every((section) => this.openSectionIds().has(section.id));
  });

  toggleAll(): void {
    this.openSectionIds.set(
      this.allExpanded() ? new Set<number>() : new Set(this.sections().map((section) => section.id))
    );
  }
}
