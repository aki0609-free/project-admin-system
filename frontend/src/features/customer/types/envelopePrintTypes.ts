export type EnvelopeType = 'NAGA3' | 'KAKU2'

export type EnvelopePrintCustomerOption = {
  id: number
  name: string
  address: string
}

export type EnvelopePrintPayload = {
  customerIds: number[]
  envelopeType: EnvelopeType
  stamp: string
  honorific: string
  fontFamily: string
  fontSize: number
}