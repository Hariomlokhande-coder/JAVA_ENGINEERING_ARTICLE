import { HttpErrorResponse } from '@angular/common/http';

import { ApiError } from '../models/api-error';

/** Turns any HTTP failure into one sentence that can be shown to the visitor. */
export function toErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'The server is not reachable. Check that the backend is running.';
  }

  const body = error.error as ApiError | string | null;

  if (body && typeof body === 'object') {
    const fieldErrors = body.fieldErrors;
    if (fieldErrors) {
      const messages = Object.values(fieldErrors);
      if (messages.length > 0) {
        return messages.join(' ');
      }
    }
    if (body.message) {
      return body.message;
    }
  }

  if (typeof body === 'string' && body.trim().length > 0) {
    return body;
  }

  return fallback;
}
