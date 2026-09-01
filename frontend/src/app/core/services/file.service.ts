import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UploadResult } from '../../models/api-error';

@Injectable({ providedIn: 'root' })
export class FileService {
  constructor(private readonly http: HttpClient) {}

  upload(file: File): Observable<UploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UploadResult>(`${environment.apiUrl}/files/upload`, formData);
  }

  /** Turns a stored path such as /uploads/articles/x.png into a URL the browser can load. */
  resolveUrl(url: string | null | undefined): string {
    if (!url) {
      return '';
    }
    if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) {
      return url;
    }
    return `${environment.filesUrl}${url.startsWith('/') ? url : `/${url}`}`;
  }
}
