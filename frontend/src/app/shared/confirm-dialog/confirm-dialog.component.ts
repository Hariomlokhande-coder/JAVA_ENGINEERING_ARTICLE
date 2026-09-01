import { ChangeDetectionStrategy, Component, ElementRef, HostListener, ViewChild, effect, inject } from '@angular/core';

import { ConfirmService } from '../../core/services/confirm.service';

/** Single global dialog host, mounted once by the root component. */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfirmDialogComponent {
  private readonly confirmService = inject(ConfirmService);

  @ViewChild('cancelButton') private cancelButton?: ElementRef<HTMLButtonElement>;

  readonly request = this.confirmService.current;

  constructor() {
    effect(() => {
      const open = this.request() !== null;
      document.body.style.overflow = open ? 'hidden' : '';
      if (open) {
        // Focus lands on Cancel so a stray Enter never destroys anything.
        setTimeout(() => this.cancelButton?.nativeElement.focus());
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.request()) {
      this.respond(false);
    }
  }

  respond(confirmed: boolean): void {
    this.confirmService.respond(confirmed);
  }
}
