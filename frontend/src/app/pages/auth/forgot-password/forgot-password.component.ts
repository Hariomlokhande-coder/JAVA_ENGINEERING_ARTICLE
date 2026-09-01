import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { toErrorMessage } from '../../../core/error-message';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: '../verify-email/verify-email.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ForgotPasswordComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly submitting = signal(false);
  readonly sentMessage = signal('');
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');

    this.authService.forgotPassword(this.form.getRawValue().email).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.sentMessage.set(response.message);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(toErrorMessage(error, 'The request could not be sent.'));
      }
    });
  }

  get emailInvalid(): boolean {
    const field = this.form.controls.email;
    return field.invalid && (field.dirty || field.touched);
  }
}
