import { Injectable, signal } from '@angular/core';

export interface PromptField {
  name: string;
  label: string;
  placeholder?: string;
  value?: string;
}

export interface PromptRequest {
  title: string;
  fields: PromptField[];
  confirmLabel: string;
}

/** Same idea as ConfirmService, but collects one or more values instead of yes or no. */
@Injectable({ providedIn: 'root' })
export class PromptService {
  private readonly request = signal<PromptRequest | null>(null);
  private resolve: ((values: Record<string, string> | null) => void) | null = null;

  readonly current = this.request.asReadonly();

  ask(options: { title: string; fields: PromptField[]; confirmLabel?: string }):
    Promise<Record<string, string> | null> {
    this.settle(null);
    this.request.set({ confirmLabel: 'Insert', ...options });

    return new Promise((resolve) => {
      this.resolve = resolve;
    });
  }

  respond(values: Record<string, string> | null): void {
    this.request.set(null);
    this.settle(values);
  }

  private settle(values: Record<string, string> | null): void {
    const pending = this.resolve;
    this.resolve = null;
    pending?.(values);
  }
}
