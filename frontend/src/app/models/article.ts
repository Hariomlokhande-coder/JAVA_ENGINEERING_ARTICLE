export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface ArticleSummary {
  id: number;
  title: string;
  slug: string;
  description: string | null;
  displayOrder: number;
  thumbnailUrl: string | null;
  githubUrl: string | null;
  youtubeUrl: string | null;
  published: boolean;
  difficulty: Difficulty;
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface Article extends ArticleSummary {
  content: string;
}

export interface ArticlePayload {
  title: string;
  slug?: string | null;
  description?: string | null;
  content: string;
  categoryId: number;
  displayOrder?: number | null;
  githubUrl?: string | null;
  youtubeUrl?: string | null;
  thumbnailUrl?: string | null;
  published: boolean;
  difficulty: Difficulty;
  tags: string[];
}
