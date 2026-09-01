import { ArticleSummary } from './article';

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  displayOrder: number;
  articleCount: number;
  createdAt: string;
  updatedAt: string;
}

/** Category together with its articles (roadmap accordion and category page). */
export interface CategoryDetail {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  displayOrder: number;
  articles: ArticleSummary[];
}

export interface CategoryPayload {
  name: string;
  slug?: string | null;
  description?: string | null;
  displayOrder?: number | null;
}
