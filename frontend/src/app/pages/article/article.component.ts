import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  Component,
  HostListener,
  OnDestroy,
  computed,
  inject,
  input,
  signal
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DatePipe, TitleCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DomSanitizer, Meta, SafeResourceUrl, Title } from '@angular/platform-browser';
import { EMPTY, catchError, forkJoin, of, switchMap } from 'rxjs';

import { CategoryService } from '../../core/services/category.service';

import { toErrorMessage } from '../../core/error-message';
import { ArticleService } from '../../core/services/article.service';
import { FileService } from '../../core/services/file.service';
import { MarkdownService, RenderedMarkdown, TocItem } from '../../core/services/markdown.service';
import { ProgressService } from '../../core/services/progress.service';
import { Article, ArticleSummary } from '../../models/article';
import { ArticleCardComponent } from '../../shared/article-card/article-card.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { LoadingComponent } from '../../shared/loading/loading.component';
import { MarkdownComponent } from '../../shared/markdown/markdown.component';

@Component({
  selector: 'app-article',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    TitleCasePipe,
    ArticleCardComponent,
    EmptyStateComponent,
    LoadingComponent,
    MarkdownComponent
  ],
  templateUrl: './article.component.html',
  styleUrl: './article.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ArticleComponent implements AfterViewChecked, OnDestroy {
  private readonly articleService = inject(ArticleService);
  private readonly markdownService = inject(MarkdownService);
  private readonly progressService = inject(ProgressService);
  private readonly fileService = inject(FileService);
  private readonly titleService = inject(Title);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly metaService = inject(Meta);
  private readonly categoryService = inject(CategoryService);

  /** Bound from the :slug route parameter by withComponentInputBinding. */
  readonly slug = input.required<string>();

  readonly article = signal<Article | null>(null);
  readonly related = signal<ArticleSummary[]>([]);
  readonly rendered = signal<RenderedMarkdown | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal('');

  readonly toc = computed<TocItem[]>(() => this.rendered()?.toc ?? []);
  readonly activeHeading = signal('');
  readonly readProgress = signal(0);
  /** Every topic of the category, shown in the left rail like a course outline. */
  readonly sectionArticles = signal<ArticleSummary[]>([]);
  readonly previousArticle = signal<ArticleSummary | null>(null);
  readonly nextArticle = signal<ArticleSummary | null>(null);
  readonly linkCopied = signal(false);
  readonly reachedEnd = signal(false);

  /** Turns a watch or short YouTube link into an embeddable player URL. */
  readonly videoEmbedUrl = computed<SafeResourceUrl | null>(() => {
    const url = this.article()?.youtubeUrl;
    if (!url) {
      return null;
    }
    const match = url.match(/(?:youtu\.be\/|[?&]v=|\/embed\/)([\w-]{6,})/);
    return match
      ? this.sanitizer.bypassSecurityTrustResourceUrl(`https://www.youtube.com/embed/${match[1]}`)
      : null;
  });

  /** Rough reading time, the usual 200 words per minute. */
  readonly readingMinutes = computed(() => {
    const content = this.article()?.content ?? '';
    const words = content.trim().split(/\s+/).filter(Boolean).length;
    return Math.max(1, Math.round(words / 200));
  });

  private observer: IntersectionObserver | null = null;
  private observedIds = '';

  readonly isCompleted = computed(() => {
    this.progressService.completedIds();
    const current = this.article();
    return current ? this.progressService.isCompleted(current.id) : false;
  });

  constructor() {
    toObservable(this.slug)
      .pipe(
        switchMap((slug) => {
          this.loading.set(true);
          this.errorMessage.set('');
          this.article.set(null);
          this.rendered.set(null);
          this.related.set([]);

          this.sectionArticles.set([]);
          this.previousArticle.set(null);
          this.nextArticle.set(null);
          this.readProgress.set(0);

          return forkJoin({
            article: this.articleService.findBySlug(slug),
            // Related articles are a nice extra, a failure there must not hide the article.
            related: this.articleService.findRelated(slug).pipe(catchError(() => of([] as ArticleSummary[])))
          }).pipe(
            catchError((error: unknown) => {
              this.errorMessage.set(toErrorMessage(error, 'This article could not be loaded.'));
              this.loading.set(false);
              return EMPTY;
            })
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(({ article, related }) => {
        this.article.set(article);
        this.related.set(related);
        this.rendered.set(this.markdownService.render(article.content));
        this.titleService.setTitle(`${article.title} | Technical Blog`);
        const summary = article.description ?? `${article.title} explained with examples and code.`;
        this.metaService.updateTag({ name: 'description', content: summary });
        this.metaService.updateTag({ property: 'og:title', content: article.title });
        this.metaService.updateTag({ property: 'og:description', content: summary });
        this.metaService.updateTag({ name: 'twitter:title', content: article.title });
        this.metaService.updateTag({ name: 'twitter:description', content: summary });
        this.loadSection(article);
        this.loading.set(false);
      });
  }

  /** Rebuilds the scroll spy once the rendered headings are in the DOM. */
  ngAfterViewChecked(): void {
    const ids = this.toc().map((item) => item.id).join(',');
    if (ids === this.observedIds) {
      return;
    }
    this.observedIds = ids;
    this.observeHeadings();
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private observeHeadings(): void {
    this.observer?.disconnect();
    if (this.toc().length === 0 || typeof IntersectionObserver === 'undefined') {
      return;
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];
        if (visible?.target.id) {
          this.activeHeading.set(visible.target.id);
        }
      },
      { rootMargin: '-88px 0px -70% 0px', threshold: 0 }
    );

    this.toc().forEach((item) => {
      const heading = document.getElementById(item.id);
      if (heading) {
        this.observer?.observe(heading);
      }
    });
  }

  /**
   * Loads the rest of the category so the reader gets the section outline
   * plus previous and next links in the roadmap order.
   */
  private loadSection(article: Article): void {
    this.categoryService
      .findBySlug(article.categorySlug)
      .pipe(catchError(() => EMPTY))
      .subscribe((category) => {
        const topics = category.articles;
        this.sectionArticles.set(topics);

        const index = topics.findIndex((item) => item.id === article.id);
        if (index === -1) {
          return;
        }
        this.previousArticle.set(index > 0 ? topics[index - 1] : null);
        this.nextArticle.set(index < topics.length - 1 ? topics[index + 1] : null);
      });
  }

  /** Position of the open article inside its section, shown as "3 of 8". */
  readonly chapterIndex = computed(() => {
    const current = this.article();
    const topics = this.sectionArticles();
    if (!current || topics.length === 0) {
      return 0;
    }
    return topics.findIndex((item) => item.id === current.id) + 1;
  });

  isRead(articleId: number): boolean {
    this.progressService.completedIds();
    return this.progressService.isCompleted(articleId);
  }

  /** Share helper: copies the current article URL. */
  copyLink(): void {
    navigator.clipboard
      ?.writeText(window.location.href)
      .then(() => {
        this.linkCopied.set(true);
        setTimeout(() => this.linkCopied.set(false), 1600);
      })
      .catch(() => this.linkCopied.set(false));
  }

  /** Drives the thin progress bar under the header. */
  @HostListener('window:scroll')
  onScroll(): void {
    const doc = document.documentElement;
    const scrollable = doc.scrollHeight - doc.clientHeight;
    const percent = scrollable <= 0 ? 0 : Math.min(100, Math.round((doc.scrollTop / scrollable) * 100));
    this.readProgress.set(percent);
    this.reachedEnd.set(percent >= 98);
  }

  thumbnail(): string {
    return this.fileService.resolveUrl(this.article()?.thumbnailUrl);
  }

  toggleCompleted(): void {
    const current = this.article();
    if (current) {
      this.progressService.toggle(current.id);
    }
  }

  /** Scrolls to a heading without pushing a new history entry. */
  scrollTo(id: string, event: Event): void {
    event.preventDefault();
    this.activeHeading.set(id);
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
