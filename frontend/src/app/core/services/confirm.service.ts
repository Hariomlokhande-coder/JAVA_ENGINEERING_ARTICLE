import { Injectable, signal } from '@angular/core';

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string;
  danger: boolean;
}

export type ConfirmOptions = Partial<ConfirmRequest> & Pick<ConfirmRequest, 'title'>;

/**
 * Replaces the native window.confirm with an in-app dialog.
 * ask() resolves once the person answers, so callers read like the browser version.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly request = signal<ConfirmRequest | null>(null);
  private resolve: ((confirmed: boolean) => void) | null = null;

  readonly current = this.request.asReadonly();

  ask(options: ConfirmOptions): Promise<boolean> {
    // A second request cancels whatever was still open, so no caller is left waiting.
    this.settle(false);

    this.request.set({
      message: '',
      confirmLabel: 'Confirm',
      cancelLabel: 'Cancel',
      danger: false,
      ...options
    });

    return new Promise<boolean>((resolve) => {
      this.resolve = resolve;
    });
  }

  respond(confirmed: boolean): void {
    this.request.set(null);
    this.settle(confirmed);
  }

  private settle(confirmed: boolean): void {
    const pending = this.resolve;
    this.resolve = null;
    pending?.(confirmed);
  }
}
