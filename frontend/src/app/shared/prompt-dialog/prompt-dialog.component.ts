import { ChangeDetectionStrategy, Component, ElementRef, HostListener, ViewChild, effect, inject } from '@angular/core';

import { PromptService } from '../../core/services/prompt.service';

/** Global input dialog, mounted once by the root component. */
@Component({
  selector: 'app-prompt-dialog',
  standalone: true,
  templateUrl: './prompt-dialog.component.html',
  styleUrl: './prompt-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PromptDialogComponent {
  private readonly promptService = inject(PromptService);

  @ViewChild('firstInput') private firstInput?: ElementRef<HTMLInputElement>;

  readonly request = this.promptService.current;
  readonly values: Record<string, string> = {};

  constructor() {
    effect(() => {
      const request = this.request();
      document.body.style.overflow = request ? 'hidden' : '';
      if (request) {
        request.fields.forEach((field) => (this.values[field.name] = field.value ?? ''));
        setTimeout(() => this.firstInput?.nativeElement.select());
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.request()) {
      this.cancel();
    }
  }

  submit(): void {
    this.promptService.respond({ ...this.values });
  }

  cancel(): void {
    this.promptService.respond(null);
  }

  update(name: string, value: string): void {
    this.values[name] = value;
  }
}
