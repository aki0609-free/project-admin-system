export const normalizeNullableString = (
  value: string | null | undefined,
): string | null => {
  if (value == null) return null

  const trimmed = value.trim()

  return trimmed === '' ? null : trimmed
}

export const truncateText = (text: string, maxLength: number, ellipsis: string = '...'): string => {
  if (text.length <= maxLength) {
    return text;
  }
  return text.slice(0, maxLength) + ellipsis;
}

export const  getLastSegment = (path: string): string => {
  // 末尾にスラッシュがある場合に備えて、一度取り除く
  const normalizedPath = path.endsWith('/') ? path.slice(0, -1) : path;
  
  // スラッシュで分割して、最後の要素を返す
  const segments = normalizedPath.split('/');
  return segments[segments.length - 1] ?? '';
}