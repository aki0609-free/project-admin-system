export type CompanyProfileForm = {
  id: number | null

  companyCode: string
  companyName: string
  companyNameKana: string
  shortName: string

  representativeTitle: string
  representativeName: string

  postalCode: string
  prefecture: string
  city: string
  addressLine1: string
  addressLine2: string

  phone: string
  fax: string
  email: string
  websiteUrl: string

  capitalAmount: number | null

  permitNumber: string
  qualifiedInvoiceIssuerNumber: string

  serviceArea: string

  businessContentsText: string
  certificationInformationText: string

  invoiceBankName: string
  invoiceBankBranchName: string
  invoiceBankAccountType: string
  invoiceBankAccountNumber: string
  invoiceBankAccountHolder: string

  invoiceNote: string

  activeFlag: boolean
}