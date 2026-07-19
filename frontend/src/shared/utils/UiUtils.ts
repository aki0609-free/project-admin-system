export function generateColors(length: number) {
  return Array.from({ length }, (_, i) => {
    const hue = (i * 360) / length
    return `hsl(${hue}, 65%, 55%)`
  })
}