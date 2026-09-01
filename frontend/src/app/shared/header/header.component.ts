import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  ViewChild,
  inject,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { ConfirmService } from '../../core/services/confirm.service';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HeaderComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly themeService = inject(ThemeService);
  private readonly confirmService = inject(ConfirmService);

  @ViewChild('searchInput') private searchInput?: ElementRef<HTMLInputElement>;

  readonly keyword = signal('');
  readonly menuOpen = signal(false);
  readonly isAdmin = this.authService.isAdmin;
  readonly isLoggedIn = this.authService.isLoggedIn;
  readonly username = this.authService.username;
  readonly theme = this.themeService.theme;

  /** Pressing / focuses the search box, Escape leaves it, the way documentation sites behave. */
  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const typingElsewhere =
      target instanceof HTMLInputElement ||
      target instanceof HTMLTextAreaElement ||
      target instanceof HTMLSelectElement ||
      target?.isContentEditable === true;

    if (event.key === '/' && !typingElsewhere && !event.ctrlKey && !event.metaKey) {
      event.preventDefault();
      this.menuOpen.set(true);
      setTimeout(() => this.searchInput?.nativeElement.focus());
      return;
    }

    if (event.key === 'Escape' && target === this.searchInput?.nativeElement) {
      this.searchInput?.nativeElement.blur();
    }
  }

  search(): void {
    const keyword = this.keyword().trim();
    if (keyword.length < 2) {
      return;
    }
    this.menuOpen.set(false);
    void this.router.navigate(['/search'], { queryParams: { q: keyword } });
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  async logout(): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: 'Sign out?',
      message: 'You will need to sign in again to create or edit content.',
      confirmLabel: 'Sign out',
      cancelLabel: 'Stay signed in'
    });

    if (!confirmed) {
      return;
    }

    this.authService.logout();
    this.closeMenu();
    void this.router.navigate(['/']);
  }
}
