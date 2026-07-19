export type CompanyProfileResponse = {
  id: number

  companyCode: string
  companyName: string
  companyNameKana: string | null
  shortName: string | null

  representativeTitle: string | null
  representativeName: string | null
  representativeDisplayName: string | null

  postalCode: string | null
  prefecture: string | null
  city: string | null
  addressLine1: string | null
  addressLine2: string | null

  fullAddress: string | null
  fullAddressWithPostalCode: string | null

  phone: string | null
  fax: string | null
  email: string | null
  websiteUrl: string | null

  capitalAmount: number | null

  permitNumber: string | null
  qualifiedInvoiceIssuerNumber: string | null

  serviceArea: string | null

  businessContents: string[]
  certificationInformation: string[]

  invoiceBankName: string | null
  invoiceBankBranchName: string | null
  invoiceBankAccountType: string | null
  invoiceBankAccountNumber: string | null
  invoiceBankAccountHolder: string | null

  invoiceBankDisplayText: string | null
  invoiceNote: string | null

  activeFlag: boolean
}

export type CompanyProfileSaveRequest = {
  companyCode: string
  companyName: string
  companyNameKana: string | null
  shortName: string | null

  representativeTitle: string | null
  representativeName: string | null

  postalCode: string | null
  prefecture: string | null
  city: string | null
  addressLine1: string | null
  addressLine2: string | null

  phone: string | null
  fax: string | null
  email: string | null
  websiteUrl: string | null

  capitalAmount: number | null

  permitNumber: string | null
  qualifiedInvoiceIssuerNumber: string | null

  serviceArea: string | null

  businessContents: string[]
  certificationInformation: string[]

  invoiceBankName: string | null
  invoiceBankBranchName: string | null
  invoiceBankAccountType: string | null
  invoiceBankAccountNumber: string | null
  invoiceBankAccountHolder: string | null

  invoiceNote: string | null

  activeFlag: boolean
}