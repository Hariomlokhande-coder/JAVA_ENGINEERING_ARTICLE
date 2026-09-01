import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuthSession,
  LoginPayload,
  LoginResponse,
  MessageResponse,
  RegisterPayload
} from '../../models/auth';

const STORAGE_KEY = 'technical-blog.session';

/** Holds the admin session. The token is the only thing the backend trusts. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly session = signal<AuthSession | null>(this.readStoredSession());

  readonly currentSession = this.session.asReadonly();
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.role === 'ADMIN');
  readonly username = computed(() => this.session()?.username ?? '');

  constructor(private readonly http: HttpClient) {}

  login(payload: LoginPayload): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, payload).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  /** Creates the account. The reader must confirm their email before signing in. */
  register(payload: RegisterPayload): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${environment.apiUrl}/auth/register`, payload);
  }

  verifyEmail(token: string): Observable<MessageResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.get<MessageResponse>(`${environment.apiUrl}/auth/verify-email`, { params });
  }

  resendVerification(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${environment.apiUrl}/auth/resend-verification`, { email });
  }

  forgotPassword(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${environment.apiUrl}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, password: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${environment.apiUrl}/auth/reset-password`, { token, password });
  }

  logout(): void {
    this.session.set(null);
    this.removeStoredSession();
  }

  token(): string | null {
    const active = this.activeSession();
    return active ? active.token : null;
  }

  /** Returns the session only while the token is still valid, clearing it once it expires. */
  activeSession(): AuthSession | null {
    const current = this.session();
    if (!current) {
      return null;
    }
    if (this.isExpired(current)) {
      this.logout();
      return null;
    }
    return current;
  }

  private storeSession(response: LoginResponse): void {
    const session: AuthSession = {
      token: response.token,
      role: response.role,
      username: response.username,
      email: response.email,
      expiresAt: response.expiresAt
    };
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Storage can be unavailable in private mode. The session then lives for this tab only.
    }
  }

  private readStoredSession(): AuthSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw) as AuthSession;
      if (!parsed?.token || !parsed?.role || this.isExpired(parsed)) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return parsed;
    } catch {
      return null;
    }
  }

  private removeStoredSession(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nothing to clean up when storage is unavailable.
    }
  }

  private isExpired(session: AuthSession): boolean {
    const expiry = Date.parse(session.expiresAt);
    return Number.isNaN(expiry) || expiry <= Date.now();
  }
}
