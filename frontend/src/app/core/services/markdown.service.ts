import { Injectable } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';
import { marked } from 'marked';
import Prism from 'prismjs';

import 'prismjs/components/prism-java';
import 'prismjs/components/prism-json';
import 'prismjs/components/prism-sql';
import 'prismjs/components/prism-bash';
import 'prismjs/components/prism-typescript';

import { environment } from '../../../environments/environment';

const LANGUAGE_LABELS: Record<string, string> = {
  java: 'Java',
  typescript: 'TypeScript',
  ts: 'TypeScript',
  javascript: 'JavaScript',
  js: 'JavaScript',
  json: 'JSON',
  sql: 'SQL',
  bash: 'Shell',
  shell: 'Shell',
  html: 'HTML',
  css: 'CSS',
  xml: 'XML',
  yaml: 'YAML'
};

export interface TocItem {
  id: string;
  text: string;
  level: number;
}

export interface RenderedMarkdown {
  html: SafeHtml;
  toc: TocItem[];
}

/**
 * Markdown to HTML for article content.
 * The output is sanitized with DOMPurify before it is trusted, then headings get anchors,
 * code blocks get highlighted and relative image paths are pointed at the API host.
 */
@Injectable({ providedIn: 'root' })
export class MarkdownService {
  constructor(private readonly sanitizer: DomSanitizer) {
    marked.setOptions({ gfm: true, breaks: false });
  }

  render(markdown: string | null | undefined): RenderedMarkdown {
    if (!markdown || markdown.trim().length === 0) {
      return { html: this.sanitizer.bypassSecurityTrustHtml(''), toc: [] };
    }

    const rawHtml = marked.parse(markdown, { async: false }) as string;
    const cleanHtml = DOMPurify.sanitize(rawHtml, { USE_PROFILES: { html: true } });

    const template = document.createElement('div');
    template.innerHTML = cleanHtml;

    const toc = this.buildToc(template);
    this.resolveImageSources(template);
    this.hardenExternalLinks(template);
    this.wrapTables(template);
    this.highlightCodeBlocks(template);

    return { html: this.sanitizer.bypassSecurityTrustHtml(template.innerHTML), toc };
  }

  private buildToc(root: HTMLElement): TocItem[] {
    const used = new Set<string>();
    const toc: TocItem[] = [];

    root.querySelectorAll('h2, h3').forEach((heading) => {
      const text = (heading.textContent ?? '').trim();
      if (!text) {
        return;
      }
      const id = this.uniqueId(text, used);
      heading.setAttribute('id', id);
      toc.push({ id, text, level: heading.tagName === 'H2' ? 2 : 3 });
    });

    return toc;
  }

  private uniqueId(text: string, used: Set<string>): string {
    const base =
      text
        .toLowerCase()
        .replace(/[^a-z0-9\s-]/g, '')
        .trim()
        .replace(/\s+/g, '-') || 'section';

    let candidate = base;
    let suffix = 2;
    while (used.has(candidate)) {
      candidate = `${base}-${suffix}`;
      suffix += 1;
    }
    used.add(candidate);
    return candidate;
  }

  private resolveImageSources(root: HTMLElement): void {
    root.querySelectorAll('img').forEach((image) => {
      const source = image.getAttribute('src');
      if (source && source.startsWith('/')) {
        image.setAttribute('src', `${environment.filesUrl}${source}`);
      }
      image.setAttribute('loading', 'lazy');
    });
  }

  private hardenExternalLinks(root: HTMLElement): void {
    root.querySelectorAll('a').forEach((link) => {
      const href = link.getAttribute('href') ?? '';
      if (/^https?:\/\//i.test(href)) {
        link.setAttribute('target', '_blank');
        link.setAttribute('rel', 'noopener noreferrer');
      }
    });
  }

  /** Wide tables scroll inside their own container instead of stretching the page. */
  private wrapTables(root: HTMLElement): void {
    root.querySelectorAll('table').forEach((table) => {
      const scroller = document.createElement('div');
      scroller.className = 'table-scroll';
      table.replaceWith(scroller);
      scroller.append(table);
    });
  }

  /**
   * Highlights each fenced block and wraps it in a figure carrying the language
   * label and a copy button. The caption is built with DOM nodes rather than
   * innerHTML so a crafted fence language can never inject markup.
   */
  private highlightCodeBlocks(root: HTMLElement): void {
    root.querySelectorAll('pre > code').forEach((block) => {
      const pre = block.parentElement;
      if (!pre) {
        return;
      }

      const languageClass = Array.from(block.classList).find((name) => name.startsWith('language-'));
      const language = languageClass ? languageClass.replace('language-', '') : 'java';
      const grammar = Prism.languages[language];

      if (grammar) {
        block.innerHTML = Prism.highlight(block.textContent ?? '', grammar, language);
      }
      pre.classList.add(`language-${language}`);

      const label = document.createElement('span');
      label.className = 'code-lang';
      label.textContent = LANGUAGE_LABELS[language] ?? language;

      const copyButton = document.createElement('button');
      copyButton.type = 'button';
      copyButton.className = 'code-copy';
      copyButton.setAttribute('data-copy', '');
      copyButton.textContent = 'Copy';

      const caption = document.createElement('figcaption');
      caption.append(label, copyButton);

      const figure = document.createElement('figure');
      figure.className = 'code-block';

      pre.replaceWith(figure);
      figure.append(caption, pre);
    });
  }
}
