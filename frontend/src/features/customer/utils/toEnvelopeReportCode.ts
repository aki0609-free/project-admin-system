import type { EnvelopeType } from "../types/envelopePrintTypes"

export const toEnvelopeReportCode = (envelopeType: EnvelopeType): string => {
  switch (envelopeType) {
    case 'NAGA3':
      return 'ENVELOPE_NAGA3'
    case 'KAKU2':
      return 'ENVELOPE_KAKU2'
    default:
      throw new Error(`Unsupported envelopeType: ${envelopeType}`)
  }
}