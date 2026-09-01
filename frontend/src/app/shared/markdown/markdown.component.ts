import { ChangeDetectionStrategy, Component, HostListener, Input, ViewEncapsulation } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';

/**
 * Renders article HTML that MarkdownService already produced and sanitized.
 * Encapsulation is disabled because the markup is injected at runtime,
 * so every rule in the stylesheet stays scoped under .markdown-body.
 */
@Component({
  selector: 'app-markdown',
  standalone: true,
  template: '<div class="markdown-body" [innerHTML]="html"></div>',
  styleUrl: './markdown.component.css',
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarkdownComponent {
  @Input({ required: true }) html: SafeHtml = '';

  /** Copy buttons are injected with the content, so the clicks are delegated from the host. */
  @HostListener('click', ['$event'])
  onClick(event: Event): void {
    const button = (event.target as HTMLElement | null)?.closest<HTMLButtonElement>('[data-copy]');
    if (!button) {
      return;
    }

    const code = button.closest('.code-block')?.querySelector('code')?.textContent ?? '';
    if (!code) {
      return;
    }

    navigator.clipboard
      ?.writeText(code)
      .then(() => this.flash(button, 'Copied'))
      .catch(() => this.flash(button, 'Press Ctrl+C'));
  }

  private flash(button: HTMLButtonElement, message: string): void {
    button.textContent = message;
    button.classList.add('is-copied');
    setTimeout(() => {
      button.textContent = 'Copy';
      button.classList.remove('is-copied');
    }, 1600);
  }
}
