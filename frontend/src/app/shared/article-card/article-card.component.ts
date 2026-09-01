import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { FileService } from '../../core/services/file.service';
import { ArticleSummary } from '../../models/article';

@Component({
  selector: 'app-article-card',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './article-card.component.html',
  styleUrl: './article-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ArticleCardComponent {
  private readonly fileService = inject(FileService);

  @Input({ required: true }) article!: ArticleSummary;

  thumbnail(): string {
    return this.fileService.resolveUrl(this.article.thumbnailUrl);
  }
}
