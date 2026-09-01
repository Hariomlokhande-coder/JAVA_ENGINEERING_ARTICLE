import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  ViewChild,
  forwardRef,
  inject,
  signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { toErrorMessage } from '../../core/error-message';
import { FileService } from '../../core/services/file.service';
import { MarkdownHtmlService } from '../../core/services/markdown-html.service';
import { PromptService } from '../../core/services/prompt.service';

/**
 * Visual article editor. What the writer sees is the finished article: real headings,
 * real bold text and real images. The value handed to the form stays Markdown, so the
 * API and the public article page are unaffected.
 */
@Component({
  selector: 'app-rich-editor',
  standalone: true,
  templateUrl: './rich-editor.component.html',
  styleUrl: './rich-editor.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RichEditorComponent),
      multi: true
    }
  ]
})
export class RichEditorComponent implements ControlValueAccessor, AfterViewInit {
  private readonly fileService = inject(FileService);
  private readonly promptService = inject(PromptService);
  private readonly markdownHtml = inject(MarkdownHtmlService);

  @ViewChild('surface', { static: true }) private surface!: ElementRef<HTMLDivElement>;

  readonly uploading = signal(0);
  readonly uploadError = signal('');
  readonly dragging = signal(false);
  disabled = false;

  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;
  private pendingValue: string | null = null;
  private savedRange: Range | null = null;

  ngAfterViewInit(): void {
    if (this.pendingValue !== null) {
      this.surface.nativeElement.innerHTML = this.markdownHtml.toHtml(this.pendingValue);
      this.pendingValue = null;
    }
  }

  writeValue(value: string | null): void {
    if (!this.surface?.nativeElement) {
      this.pendingValue = value ?? '';
      return;
    }
    // Only rewrite when the incoming value differs, so typing is never interrupted.
    const current = this.markdownHtml.toMarkdown(this.surface.nativeElement.innerHTML).trim();
    if (current !== (value ?? '').trim()) {
      this.surface.nativeElement.innerHTML = this.markdownHtml.toHtml(value);
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onInput(): void {
    this.emit();
  }

  onBlur(): void {
    this.rememberSelection();
    this.onTouched();
  }

  onKeydown(event: KeyboardEvent): void {
    if (!event.ctrlKey && !event.metaKey) {
      return;
    }
    const key = event.key.toLowerCase();
    if (key === 'b') {
      event.preventDefault();
      this.command('bold');
    } else if (key === 'i') {
      event.preventDefault();
      this.command('italic');
    } else if (key === 'k') {
      event.preventDefault();
      void this.insertLink();
    }
  }

  /** Pasted images upload straight away, other content is inserted as plain text. */
  onPaste(event: ClipboardEvent): void {
    const images = Array.from(event.clipboardData?.files ?? []).filter((file) =>
      file.type.startsWith('image/')
    );

    if (images.length > 0) {
      event.preventDefault();
      images.forEach((file) => this.uploadAndInsert(file));
      return;
    }

    const text = event.clipboardData?.getData('text/plain');
    if (text) {
      event.preventDefault();
      this.command('insertText', text);
    }
  }

  onDragOver(event: DragEvent): void {
    if (event.dataTransfer?.types.includes('Files')) {
      event.preventDefault();
      this.dragging.set(true);
    }
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    this.dragging.set(false);
    const images = Array.from(event.dataTransfer?.files ?? []).filter((file) =>
      file.type.startsWith('image/')
    );
    if (images.length === 0) {
      return;
    }
    event.preventDefault();
    images.forEach((file) => this.uploadAndInsert(file));
  }

  onFilePicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.uploadAndInsert(file);
    }
    input.value = '';
  }

  // Toolbar

  bold(): void {
    this.command('bold');
  }

  italic(): void {
    this.command('italic');
  }

  heading(level: 2 | 3): void {
    this.command('formatBlock', `<h${level}>`);
  }

  paragraph(): void {
    this.command('formatBlock', '<p>');
  }

  bulletList(): void {
    this.command('insertUnorderedList');
  }

  numberedList(): void {
    this.command('insertOrderedList');
  }

  quote(): void {
    this.command('formatBlock', '<blockquote>');
  }

  codeBlock(): void {
    this.restoreSelection();
    const selected = window.getSelection()?.toString() || '// your code here';
    const pre = document.createElement('pre');
    const code = document.createElement('code');
    code.className = 'language-java';
    code.textContent = selected;
    pre.appendChild(code);
    this.insertNode(pre);
  }

  async insertLink(): Promise<void> {
    this.rememberSelection();
    const selected = window.getSelection()?.toString() ?? '';

    const values = await this.promptService.ask({
      title: 'Insert link',
      confirmLabel: 'Insert link',
      fields: [
        { name: 'url', label: 'URL', placeholder: 'https://example.com' },
        { name: 'text', label: 'Text to show', placeholder: 'link text', value: selected }
      ]
    });

    const url = values?.['url']?.trim();
    if (!url) {
      return;
    }

    this.restoreSelection();
    const label = values?.['text']?.trim() || url;
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.textContent = label;
    this.insertNode(anchor);
  }

  async insertImageUrl(): Promise<void> {
    this.rememberSelection();
    const values = await this.promptService.ask({
      title: 'Insert image by URL',
      confirmLabel: 'Insert image',
      fields: [
        { name: 'url', label: 'Image URL', placeholder: 'https://example.com/diagram.png' },
        { name: 'alt', label: 'Alt text', placeholder: 'what the image shows' }
      ]
    });

    const url = values?.['url']?.trim();
    if (!url) {
      return;
    }
    this.restoreSelection();
    this.insertImageNode(url, values?.['alt']?.trim() || 'image');
  }

  // Internals

  private command(name: string, value?: string): void {
    this.surface.nativeElement.focus();
    this.restoreSelection();
    document.execCommand(name, false, value);
    this.emit();
  }

  private insertNode(node: Node): void {
    this.surface.nativeElement.focus();
    const selection = window.getSelection();
    const range = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null;

    if (range && this.surface.nativeElement.contains(range.commonAncestorContainer)) {
      range.deleteContents();
      range.insertNode(node);
      range.setStartAfter(node);
      range.collapse(true);
      selection?.removeAllRanges();
      selection?.addRange(range);
    } else {
      this.surface.nativeElement.appendChild(node);
    }

    this.emit();
  }

  /**
   * Shows the picture immediately from a local preview, then swaps in the stored URL
   * once the upload finishes, so writing never has to pause.
   */
  private uploadAndInsert(file: File): void {
    const previewUrl = URL.createObjectURL(file);
    const image = this.insertImageNode(previewUrl, file.name.replace(/\.[^.]+$/, ''));
    image.classList.add('is-uploading');

    this.uploadError.set('');
    this.uploading.update((count) => count + 1);

    this.fileService.upload(file).subscribe({
      next: (result) => {
        this.uploading.update((count) => count - 1);
        image.src = this.fileService.resolveUrl(result.url);
        image.classList.remove('is-uploading');
        URL.revokeObjectURL(previewUrl);
        this.emit();
      },
      error: (error: unknown) => {
        this.uploading.update((count) => count - 1);
        this.uploadError.set(toErrorMessage(error, 'The image could not be uploaded.'));
        image.remove();
        URL.revokeObjectURL(previewUrl);
        this.emit();
      }
    });
  }

  private insertImageNode(src: string, alt: string): HTMLImageElement {
    const image = document.createElement('img');
    image.src = src;
    image.alt = alt;
    this.insertNode(image);
    return image;
  }

  private rememberSelection(): void {
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      if (this.surface.nativeElement.contains(range.commonAncestorContainer)) {
        this.savedRange = range.cloneRange();
      }
    }
  }

  /**
   * The caret is lost while a dialog is open, so it is put back before inserting.
   * A caret that is still inside the editor is left exactly where the writer put it.
   */
  private restoreSelection(): void {
    const selection = window.getSelection();
    const insideEditor =
      selection !== null &&
      selection.rangeCount > 0 &&
      this.surface.nativeElement.contains(selection.getRangeAt(0).commonAncestorContainer);

    if (insideEditor || !this.savedRange) {
      return;
    }
    selection?.removeAllRanges();
    selection?.addRange(this.savedRange);
  }

  private emit(): void {
    this.rememberSelection();
    this.onChange(this.markdownHtml.toMarkdown(this.surface.nativeElement.innerHTML));
  }
}
