import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { toErrorMessage } from '../../../core/error-message';
import { AuthService } from '../../../core/services/auth.service';

/** Landing page for the link in the confirmation email. */
@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  readonly state = signal<'checking' | 'done' | 'failed'>('checking');
  readonly message = signal('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('failed');
      this.message.set('This link is missing its token. Open the link from the email again.');
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (response) => {
        this.state.set('done');
        this.message.set(response.message);
      },
      error: (error: unknown) => {
        this.state.set('failed');
        this.message.set(toErrorMessage(error, 'This link is not valid or has already been used.'));
      }
    });
  }
}
