import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { toErrorMessage } from '../../../core/error-message';
import { AuthService } from '../../../core/services/auth.service';

function passwordStrength(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '');
  if (value.length === 0) {
    return null;
  }
  return /[A-Za-z]/.test(value) && /\d/.test(value) ? null : { weak: true };
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: '../verify-email/verify-email.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResetPasswordComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly showPassword = signal(false);
  readonly token = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8), passwordStrength]]
  });

  ngOnInit(): void {
    this.token.set(this.route.snapshot.queryParamMap.get('token') ?? '');
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.token()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');

    this.authService.resetPassword(this.token(), this.form.getRawValue().password).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigate(['/login'], { queryParams: { reason: 'password-changed' } });
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(toErrorMessage(error, 'The password could not be changed.'));
      }
    });
  }

  togglePassword(): void {
    this.showPassword.update((shown) => !shown);
  }

  get passwordInvalid(): boolean {
    const field = this.form.controls.password;
    return field.invalid && (field.dirty || field.touched);
  }
}
