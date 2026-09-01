import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { ProgressService } from '../../../core/services/progress.service';
import { CategoryDetail } from '../../../models/category';

/** One expandable roadmap section: the category header plus a table of its topics. */
@Component({
  selector: 'app-roadmap-section',
  standalone: true,
  imports: [RouterLink, TitleCasePipe],
  templateUrl: './roadmap-section.component.html',
  styleUrl: './roadmap-section.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RoadmapSectionComponent {
  private readonly progressService = inject(ProgressService);

  @Input({ required: true }) section!: CategoryDetail;
  @Input() expanded = false;
  @Output() readonly toggled = new EventEmitter<number>();

  completedCount(): number {
    this.progressService.completedIds();
    return this.progressService.countCompleted(this.section.articles.map((article) => article.id));
  }

  /** Fraction of the section that is read, drives the bar across the card. */
  completedPercent(): number {
    const total = this.section.articles.length;
    return total === 0 ? 0 : Math.round((this.completedCount() / total) * 100);
  }

  isCompleted(articleId: number): boolean {
    this.progressService.completedIds();
    return this.progressService.isCompleted(articleId);
  }

  toggleCompleted(articleId: number): void {
    this.progressService.toggle(articleId);
  }

  isFavorite(articleId: number): boolean {
    this.progressService.favouriteIds();
    return this.progressService.isFavourite(articleId);
  }

  toggleFavorite(articleId: number): void {
    this.progressService.toggleFavourite(articleId);
  }
}
