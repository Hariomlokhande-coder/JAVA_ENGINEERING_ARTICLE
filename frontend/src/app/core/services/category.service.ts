import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Category, CategoryDetail, CategoryPayload } from '../../models/category';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly baseUrl = `${environment.apiUrl}/categories`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Category[]> {
    return this.http.get<Category[]>(this.baseUrl);
  }

  /** Every category with its articles, used by the roadmap accordion. */
  findRoadmap(): Observable<CategoryDetail[]> {
    return this.http.get<CategoryDetail[]>(`${this.baseUrl}/roadmap`);
  }

  findBySlug(slug: string): Observable<CategoryDetail> {
    return this.http.get<CategoryDetail>(`${this.baseUrl}/slug/${encodeURIComponent(slug)}`);
  }

  create(payload: CategoryPayload): Observable<Category> {
    return this.http.post<Category>(this.baseUrl, payload);
  }

  update(id: number, payload: CategoryPayload): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
