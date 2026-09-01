import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Tag } from '../../models/tag';

@Injectable({ providedIn: 'root' })
export class TagService {
  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<Tag[]> {
    return this.http.get<Tag[]>(`${environment.apiUrl}/tags`);
  }
}
