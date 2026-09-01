import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { ConfirmService } from '../services/confirm.service';

/** Implemented by forms that should warn before the user navigates away. */
export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (!component.hasUnsavedChanges()) {
    return true;
  }

  return inject(ConfirmService).ask({
    title: 'Leave without saving?',
    message: 'This article has changes that have not been saved yet.',
    confirmLabel: 'Discard changes',
    cancelLabel: 'Keep editing',
    danger: true
  });
};
