import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/** Sends an already signed in admin straight to the dashboard instead of the login form. */
export const loginRedirectGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.activeSession()) {
    return true;
  }

  return router.createUrlTree([authService.isAdmin() ? '/admin/dashboard' : '/']);
};
