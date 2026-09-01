import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ProgressEntry } from '../../models/auth';

/** Server side reading progress, used once the reader has an account. */
@Injectable({ providedIn: 'root' })
export class ProgressApiService {
  private readonly baseUrl = `${environment.apiUrl}/me/progress`;

  constructor(private readonly http: HttpClient) {}

  findMine(): Observable<ProgressEntry[]> {
    return this.http.get<ProgressEntry[]>(this.baseUrl);
  }

  save(articleId: number, change: { completed?: boolean; favourite?: boolean }): Observable<ProgressEntry> {
    return this.http.put<ProgressEntry>(`${this.baseUrl}/${articleId}`, change);
  }
}
