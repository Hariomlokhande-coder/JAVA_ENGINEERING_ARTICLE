import { ChangeDetectionStrategy, Component, HostListener, signal } from '@angular/core';

/** Appears after the reader scrolls past one screen and returns them to the top. */
@Component({
  selector: 'app-back-to-top',
  standalone: true,
  templateUrl: './back-to-top.component.html',
  styleUrl: './back-to-top.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BackToTopComponent {
  readonly visible = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.visible.set(window.scrollY > window.innerHeight);
  }

  scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
