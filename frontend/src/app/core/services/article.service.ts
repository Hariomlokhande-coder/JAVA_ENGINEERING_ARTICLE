import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Article, ArticlePayload, ArticleSummary } from '../../models/article';
import { Page } from '../../models/page';

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly baseUrl = `${environment.apiUrl}/articles`;

  constructor(private readonly http: HttpClient) {}

  findPublished(page = 0, size = 9): Observable<Page<ArticleSummary>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ArticleSummary>>(this.baseUrl, { params });
  }

  search(keyword: string, page = 0, size = 10): Observable<Page<ArticleSummary>> {
    const params = new HttpParams().set('keyword', keyword).set('page', page).set('size', size);
    return this.http.get<Page<ArticleSummary>>(`${this.baseUrl}/search`, { params });
  }

  /** Admin listing including drafts. */
  findForAdmin(keyword: string | null, page = 0, size = 10): Observable<Page<ArticleSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (keyword && keyword.trim().length > 0) {
      params = params.set('keyword', keyword.trim());
    }
    return this.http.get<Page<ArticleSummary>>(`${this.baseUrl}/manage`, { params });
  }

  findByCategory(categorySlug: string): Observable<ArticleSummary[]> {
    return this.http.get<ArticleSummary[]>(`${this.baseUrl}/category/${encodeURIComponent(categorySlug)}`);
  }

  findBySlug(slug: string): Observable<Article> {
    return this.http.get<Article>(`${this.baseUrl}/slug/${encodeURIComponent(slug)}`);
  }

  findRelated(slug: string): Observable<ArticleSummary[]> {
    return this.http.get<ArticleSummary[]>(`${this.baseUrl}/slug/${encodeURIComponent(slug)}/related`);
  }

  findById(id: number): Observable<Article> {
    return this.http.get<Article>(`${this.baseUrl}/${id}`);
  }

  create(payload: ArticlePayload): Observable<Article> {
    return this.http.post<Article>(this.baseUrl, payload);
  }

  update(id: number, payload: ArticlePayload): Observable<Article> {
    return this.http.put<Article>(`${this.baseUrl}/${id}`, payload);
  }

  setPublished(id: number, published: boolean): Observable<Article> {
    const params = new HttpParams().set('published', published);
    return this.http.patch<Article>(`${this.baseUrl}/${id}/publish`, null, { params });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
