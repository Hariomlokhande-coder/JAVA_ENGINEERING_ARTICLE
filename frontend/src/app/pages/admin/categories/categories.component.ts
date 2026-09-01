import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { toErrorMessage } from '../../../core/error-message';
import { CategoryService } from '../../../core/services/category.service';
import { ConfirmService } from '../../../core/services/confirm.service';
import { Category } from '../../../models/category';
import { EmptyStateComponent } from '../../../shared/empty-state/empty-state.component';
import { LoadingComponent } from '../../../shared/loading/loading.component';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, EmptyStateComponent, LoadingComponent],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoriesComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly categoryService = inject(CategoryService);
  private readonly confirmService = inject(ConfirmService);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly deletingId = signal<number | null>(null);
  readonly editingId = signal<number | null>(null);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    slug: ['', [Validators.pattern(/^$|^[a-z0-9]+(?:-[a-z0-9]+)*$/), Validators.maxLength(140)]],
    description: ['', Validators.maxLength(500)],
    displayOrder: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.categoryService.findAll().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(toErrorMessage(error, 'Categories could not be loaded.'));
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      slug: raw.slug.trim() || null,
      description: raw.description.trim() || null,
      displayOrder: raw.displayOrder
    };

    this.saving.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const editingId = this.editingId();
    const request = editingId
      ? this.categoryService.update(editingId, payload)
      : this.categoryService.create(payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set(editingId ? 'Category updated.' : 'Category created.');
        this.resetForm();
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.errorMessage.set(toErrorMessage(error, 'The category could not be saved.'));
      }
    });
  }

  edit(category: Category): void {
    this.editingId.set(category.id);
    this.errorMessage.set('');
    this.successMessage.set('');
    this.form.setValue({
      name: category.name,
      slug: category.slug,
      description: category.description ?? '',
      displayOrder: category.displayOrder
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async remove(category: Category): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: `Delete "${category.name}"?`,
      message: 'This category will be removed permanently.',
      confirmLabel: 'Delete category',
      danger: true
    });

    if (!confirmed) {
      return;
    }

    this.deletingId.set(category.id);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.categoryService.delete(category.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.successMessage.set(`"${category.name}" was deleted.`);
        if (this.editingId() === category.id) {
          this.resetForm();
        }
        this.load();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.errorMessage.set(toErrorMessage(error, 'The category could not be deleted.'));
      }
    });
  }

  resetForm(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', slug: '', description: '', displayOrder: 0 });
  }

  hasError(control: 'name' | 'slug' | 'description' | 'displayOrder'): boolean {
    const field = this.form.controls[control];
    return field.invalid && (field.dirty || field.touched);
  }
}
