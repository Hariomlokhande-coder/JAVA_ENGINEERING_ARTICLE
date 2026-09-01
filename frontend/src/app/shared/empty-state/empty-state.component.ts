import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** Shared placeholder for empty lists and for recoverable errors. */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyStateComponent {
  @Input({ required: true }) title = '';
  @Input() description = '';
  @Input() icon = '';
}
