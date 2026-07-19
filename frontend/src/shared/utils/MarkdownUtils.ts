import { marked } from 'marked'
import { sanitizeHtml } from '@/shared/utils/HtmlUtils'

export function markdownToHtml(markdown: string | null | undefined): string {
  const html = marked.parse(markdown ?? '') as string
  return sanitizeHtml(html)
}