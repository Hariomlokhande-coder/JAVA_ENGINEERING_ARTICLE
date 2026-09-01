import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Ends the local session when the backend rejects the token, so an expired
 * login never leaves the admin area half usable. Components still receive the error.
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const isLoginCall = request.url.includes('/auth/login');

      if (error.status === 401 && !isLoginCall && authService.isLoggedIn()) {
        authService.logout();
        void router.navigate(['/login'], { queryParams: { reason: 'expired' } });
      }

      return throwError(() => error);
    })
  );
};
