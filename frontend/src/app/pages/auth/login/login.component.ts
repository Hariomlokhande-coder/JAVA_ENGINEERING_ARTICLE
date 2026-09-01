import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { toErrorMessage } from '../../../core/error-message';
import { AuthService } from '../../../core/services/auth.service';

/** How long the sign in spinner stays visible at minimum. */
const MIN_SPINNER_MS = 700;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly showPassword = signal(false);
  readonly capsLockOn = signal(false);
  readonly errorMessage = signal('');
  readonly sessionExpired = signal(this.route.snapshot.queryParamMap.get('reason') === 'expired');

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.sessionExpired.set(false);
    const startedAt = Date.now();

    this.authService.login(this.form.getRawValue()).subscribe({
      next: (response) => {
        this.afterMinimumSpin(startedAt, () => {
          this.submitting.set(false);
          const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');
          // Administrators land on the dashboard, readers go back to the roadmap.
          const fallback = response.role === 'ADMIN' ? '/admin/dashboard' : '/';
          void this.router.navigateByUrl(redirectTo ?? fallback);
        });
      },
      error: (error: unknown) => {
        this.afterMinimumSpin(startedAt, () => {
          this.submitting.set(false);
          this.errorMessage.set(toErrorMessage(error, 'Sign in failed. Please try again.'));
        });
      }
    });
  }

  togglePassword(): void {
    this.showPassword.update((shown) => !shown);
  }

  /** Warns about Caps Lock, the most common reason a correct password is rejected. */
  checkCapsLock(event: KeyboardEvent): void {
    this.capsLockOn.set(event.getModifierState?.('CapsLock') ?? false);
  }

  /**
   * A local API answers in milliseconds, which makes the spinner flash and look like nothing
   * happened. Holding it for a moment keeps the feedback readable.
   */
  private afterMinimumSpin(startedAt: number, action: () => void): void {
    const remaining = Math.max(0, MIN_SPINNER_MS - (Date.now() - startedAt));
    setTimeout(action, remaining);
  }

  hasError(control: 'email' | 'password'): boolean {
    const field = this.form.controls[control];
    return field.invalid && (field.dirty || field.touched);
  }
}
