import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { toErrorMessage } from '../../../core/error-message';
import { ArticleService } from '../../../core/services/article.service';
import { CategoryService } from '../../../core/services/category.service';
import { MarkdownService, RenderedMarkdown } from '../../../core/services/markdown.service';
import { TagService } from '../../../core/services/tag.service';
import { HasUnsavedChanges } from '../../../core/guards/unsaved-changes.guard';
import { ArticlePayload, Difficulty } from '../../../models/article';
import { Category } from '../../../models/category';
import { LoadingComponent } from '../../../shared/loading/loading.component';
import { MarkdownComponent } from '../../../shared/markdown/markdown.component';
import { RichEditorComponent } from '../../../shared/rich-editor/rich-editor.component';

const URL_PATTERN = /^$|^https?:\/\/\S+$/;
const MAX_TAGS = 15;
/** Same rules as SlugUtils on the backend, used only for the live preview. */
function toSlug(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

type FormField =
  | 'title'
  | 'slug'
  | 'description'
  | 'content'
  | 'categoryId'
  | 'displayOrder'
  | 'githubUrl'
  | 'youtubeUrl'
  | 'thumbnailUrl'
  | 'tags';

@Component({
  selector: 'app-article-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LoadingComponent, MarkdownComponent, RichEditorComponent],
  templateUrl: './article-form.component.html',
  styleUrl: './article-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ArticleFormComponent implements OnInit, HasUnsavedChanges {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly articleService = inject(ArticleService);
  private readonly categoryService = inject(CategoryService);
  private readonly tagService = inject(TagService);
  private readonly markdownService = inject(MarkdownService);

  readonly articleId = signal<number | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly knownTags = signal<string[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly showPreview = signal(false);
  readonly preview = signal<RenderedMarkdown | null>(null);

  readonly isEditMode = computed(() => this.articleId() !== null);
  private savedSuccessfully = false;

  /** Used by the route guard to warn before edits are thrown away. */
  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.savedSuccessfully && !this.saving();
  }

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    slug: ['', [Validators.pattern(/^$|^[a-z0-9]+(?:-[a-z0-9]+)*$/), Validators.maxLength(240)]],
    description: ['', Validators.maxLength(500)],
    content: ['', Validators.required],
    categoryId: [null as number | null, Validators.required],
    displayOrder: [0, [Validators.required, Validators.min(0)]],
    githubUrl: ['', Validators.pattern(URL_PATTERN)],
    youtubeUrl: ['', Validators.pattern(URL_PATTERN)],
    thumbnailUrl: ['', Validators.maxLength(500)],
    tags: ['', Validators.maxLength(400)],
    published: [false],
    difficulty: ['EASY' as Difficulty, Validators.required]
  });

  private readonly titleValue = toSignal(this.form.controls.title.valueChanges, { initialValue: '' });
  private readonly descriptionValue = toSignal(this.form.controls.description.valueChanges, { initialValue: '' });
  private readonly slugValue = toSignal(this.form.controls.slug.valueChanges, { initialValue: '' });

  readonly titleLength = computed(() => this.titleValue().length);
  readonly descriptionLength = computed(() => this.descriptionValue().length);

  /** Mirrors the slug the backend will generate, so the final URL is visible while typing. */
  readonly slugPreview = computed(() => {
    const manual = this.slugValue().trim();
    return toSlug(manual || this.titleValue());
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const parsedId = idParam ? Number(idParam) : null;

    if (idParam && (parsedId === null || Number.isNaN(parsedId))) {
      this.errorMessage.set('That article id is not valid.');
      this.loading.set(false);
      return;
    }

    this.articleId.set(parsedId);
    this.loadFormData(parsedId);
  }

  private loadFormData(articleId: number | null): void {
    this.loading.set(true);

    forkJoin({
      categories: this.categoryService.findAll(),
      tags: this.tagService.findAll(),
      article: articleId === null ? of(null) : this.articleService.findById(articleId)
    }).subscribe({
      next: ({ categories, tags, article }) => {
        this.categories.set(categories);
        this.knownTags.set(tags.map((tag) => tag.name));

        if (article) {
          this.form.setValue({
            title: article.title,
            slug: article.slug,
            description: article.description ?? '',
            content: article.content,
            categoryId: article.categoryId,
            displayOrder: article.displayOrder,
            githubUrl: article.githubUrl ?? '',
            youtubeUrl: article.youtubeUrl ?? '',
            thumbnailUrl: article.thumbnailUrl ?? '',
            tags: article.tags.join(', '),
            published: article.published,
            difficulty: article.difficulty
          });
        } else if (categories.length > 0) {
          this.form.controls.categoryId.setValue(categories[0].id);
        }

        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(toErrorMessage(error, 'The form could not be loaded.'));
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Please fix the highlighted fields before saving.');
      return;
    }

    const raw = this.form.getRawValue();
    if (raw.categoryId === null) {
      this.errorMessage.set('Choose a category for this article.');
      return;
    }

    const payload: ArticlePayload = {
      title: raw.title.trim(),
      slug: raw.slug.trim() || null,
      description: raw.description.trim() || null,
      content: raw.content,
      categoryId: raw.categoryId,
      displayOrder: raw.displayOrder,
      githubUrl: raw.githubUrl.trim() || null,
      youtubeUrl: raw.youtubeUrl.trim() || null,
      thumbnailUrl: raw.thumbnailUrl.trim() || this.firstImageIn(raw.content),
      published: raw.published,
      difficulty: raw.difficulty,
      tags: this.parseTags(raw.tags)
    };

    this.saving.set(true);
    this.errorMessage.set('');

    const id = this.articleId();
    const request = id === null ? this.articleService.create(payload) : this.articleService.update(id, payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.savedSuccessfully = true;
        void this.router.navigate(['/admin/dashboard']);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.errorMessage.set(toErrorMessage(error, 'The article could not be saved.'));
      }
    });
  }

  togglePreview(): void {
    const next = !this.showPreview();
    this.showPreview.set(next);
    if (next) {
      this.preview.set(this.markdownService.render(this.form.controls.content.value));
    }
  }

  addTag(tag: string): void {
    const current = this.parseTags(this.form.controls.tags.value);
    if (current.includes(tag.toLowerCase()) || current.length >= MAX_TAGS) {
      return;
    }
    this.form.controls.tags.setValue([...current, tag].join(', '));
  }

  hasError(control: FormField): boolean {
    const field = this.form.controls[control];
    return field.invalid && (field.dirty || field.touched);
  }

  /**
   * Cards and search results show a picture, so the first image in the article
   * becomes the thumbnail automatically. No separate upload field is needed.
   */
  private firstImageIn(content: string): string | null {
    const match = content.match(/!\[[^\]]*\]\(([^)\s]+)\)/);
    return match ? match[1] : null;
  }

  /** Splits the comma separated input, trims, lower cases and removes duplicates. */
  private parseTags(value: string): string[] {
    const seen = new Set<string>();
    return value
      .split(',')
      .map((tag) => tag.trim().toLowerCase())
      .filter((tag) => tag.length > 0)
      .filter((tag) => {
        if (seen.has(tag)) {
          return false;
        }
        seen.add(tag);
        return true;
      })
      .slice(0, MAX_TAGS);
  }
}
