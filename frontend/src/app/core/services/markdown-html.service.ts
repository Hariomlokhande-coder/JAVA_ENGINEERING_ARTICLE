import { Injectable } from '@angular/core';
import DOMPurify from 'dompurify';
import { marked } from 'marked';

import { environment } from '../../../environments/environment';

/**
 * Converts between the Markdown stored in the database and the HTML shown inside
 * the visual editor. The editor works on real elements, the API keeps Markdown,
 * so both sides stay unchanged.
 */
@Injectable({ providedIn: 'root' })
export class MarkdownHtmlService {
  /** Markdown to editable HTML. Image paths are made absolute so they render while editing. */
  toHtml(markdown: string | null | undefined): string {
    if (!markdown || markdown.trim().length === 0) {
      return '';
    }

    const raw = marked.parse(markdown, { async: false }) as string;
    const clean = DOMPurify.sanitize(raw, { USE_PROFILES: { html: true } });

    const holder = document.createElement('div');
    holder.innerHTML = clean;
    holder.querySelectorAll('img').forEach((image) => {
      const source = image.getAttribute('src') ?? '';
      if (source.startsWith('/')) {
        image.setAttribute('src', `${environment.filesUrl}${source}`);
      }
    });

    return holder.innerHTML;
  }

  /** Editable HTML back to Markdown, with image paths stored relative again. */
  toMarkdown(html: string): string {
    const holder = document.createElement('div');
    holder.innerHTML = html;

    const markdown = this.serializeChildren(holder).replace(/\n{3,}/g, '\n\n').trim();
    return markdown.length === 0 ? '' : `${markdown}\n`;
  }

  private serializeChildren(node: Node): string {
    return Array.from(node.childNodes)
      .map((child) => this.serialize(child))
      .join('');
  }

  private serialize(node: Node): string {
    if (node.nodeType === Node.TEXT_NODE) {
      return (node.textContent ?? '').replace(/\s+/g, ' ');
    }
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return '';
    }

    const element = node as HTMLElement;
    const inner = this.serializeChildren(element).trim();

    switch (element.tagName) {
      case 'H1':
        return `# ${inner}\n\n`;
      case 'H2':
        return `## ${inner}\n\n`;
      case 'H3':
        return `### ${inner}\n\n`;
      case 'H4':
        return `#### ${inner}\n\n`;
      case 'STRONG':
      case 'B':
        return inner ? `**${inner}**` : '';
      case 'EM':
      case 'I':
        return inner ? `_${inner}_` : '';
      case 'A':
        return `[${inner}](${element.getAttribute('href') ?? ''})`;
      case 'IMG': {
        const source = this.relativeSource(element);
        // A local preview is still uploading, so it is left out until the real URL arrives.
        return source.startsWith('blob:')
          ? ''
          : `![${element.getAttribute('alt') ?? 'image'}](${source})`;
      }
      case 'BR':
        return '\n';
      case 'HR':
        return '---\n\n';
      case 'CODE':
        return element.parentElement?.tagName === 'PRE' ? inner : `\`${inner}\``;
      case 'PRE':
        return this.serializePre(element);
      case 'UL':
      case 'OL':
        return this.serializeList(element);
      case 'LI':
        return inner;
      case 'BLOCKQUOTE':
        return `${inner
          .split('\n')
          .map((line) => `> ${line}`)
          .join('\n')}\n\n`;
      case 'TABLE':
        return this.serializeTable(element as HTMLTableElement);
      case 'P':
      case 'DIV':
      case 'SECTION':
        return inner ? `${inner}\n\n` : '';
      default:
        return inner;
    }
  }

  private serializePre(element: HTMLElement): string {
    const code = element.querySelector('code');
    const language =
      Array.from(code?.classList ?? [])
        .find((name) => name.startsWith('language-'))
        ?.replace('language-', '') ?? '';
    const body = (code?.textContent ?? element.textContent ?? '').replace(/\n+$/, '');
    return `\`\`\`${language}\n${body}\n\`\`\`\n\n`;
  }

  private serializeList(element: HTMLElement): string {
    const ordered = element.tagName === 'OL';
    const items = Array.from(element.children).filter((child) => child.tagName === 'LI');

    const lines = items.map((item, index) => {
      const marker = ordered ? `${index + 1}. ` : '- ';
      const text = this.serializeChildren(item).trim().replace(/\n{2,}/g, '\n');
      return `${marker}${text}`;
    });

    return `${lines.join('\n')}\n\n`;
  }

  private serializeTable(table: HTMLTableElement): string {
    const rows = Array.from(table.querySelectorAll('tr'));
    if (rows.length === 0) {
      return '';
    }

    const toCells = (row: HTMLTableRowElement) =>
      Array.from(row.cells).map((cell) => this.serializeChildren(cell).trim().replace(/\n/g, ' '));

    const header = toCells(rows[0] as HTMLTableRowElement);
    const separator = header.map(() => '---');
    const body = rows.slice(1).map((row) => toCells(row as HTMLTableRowElement));

    const render = (cells: string[]) => `| ${cells.join(' | ')} |`;
    return [render(header), render(separator), ...body.map(render)].join('\n') + '\n\n';
  }

  /** Uploaded images are stored as /uploads/... so the content survives a host change. */
  private relativeSource(element: HTMLElement): string {
    const source = element.getAttribute('src') ?? '';
    if (environment.filesUrl && source.startsWith(environment.filesUrl)) {
      return source.slice(environment.filesUrl.length);
    }
    return source;
  }
}
