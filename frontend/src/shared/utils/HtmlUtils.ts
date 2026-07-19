import DOMPurify from 'dompurify'

export function sanitizeHtml(html: string | null | undefined): string {
  return DOMPurify.sanitize(html ?? '', {
    ADD_ATTR: ['target', 'rel'],
  })
}

export function plainTextToHtml(text: string | null | undefined): string {
  return sanitizeHtml((text ?? '').replace(/\n/g, '<br />'))
}