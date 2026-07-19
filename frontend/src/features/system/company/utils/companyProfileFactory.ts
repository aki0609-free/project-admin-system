import type {
  CompanyProfileResponse,
  CompanyProfileSaveRequest,
} from '../types/companyProfileApiTypes'

import type {
  CompanyProfileForm,
} from '../types/companyProfileTypes'

const toStringValue = (
  value: string | null | undefined,
): string => value ?? ''

const toNullableString = (
  value: string | null | undefined,
): string | null => {
  const normalized = value?.trim()

  return normalized
    ? normalized
    : null
}

const splitLines = (
  value: string,
): string[] =>
  value
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)

const joinLines = (
  values: string[] | null | undefined,
): string =>
  values?.filter(Boolean).join('\n') ?? ''

export const createEmptyCompanyProfileForm =
  (): CompanyProfileForm => ({
    id: null,

    companyCode: 'DEFAULT',
    companyName: '',
    companyNameKana: '',
    shortName: '',

    representativeTitle: '',
    representativeName: '',

    postalCode: '',
    prefecture: '',
    city: '',
    addressLine1: '',
    addressLine2: '',

    phone: '',
    fax: '',
    email: '',
    websiteUrl: '',

    capitalAmount: null,

    permitNumber: '',
    qualifiedInvoiceIssuerNumber: '',

    serviceArea: '',

    businessContentsText: '',
    certificationInformationText: '',

    invoiceBankName: '',
    invoiceBankBranchName: '',
    invoiceBankAccountType: '',
    invoiceBankAccountNumber: '',
    invoiceBankAccountHolder: '',

    invoiceNote: '',

    activeFlag: true,
  })

export const toCompanyProfileForm = (
  response: CompanyProfileResponse,
): CompanyProfileForm => ({
  id: response.id,

  companyCode: response.companyCode,
  companyName: response.companyName,
  companyNameKana:
    toStringValue(response.companyNameKana),
  shortName:
    toStringValue(response.shortName),

  representativeTitle:
    toStringValue(response.representativeTitle),
  representativeName:
    toStringValue(response.representativeName),

  postalCode:
    toStringValue(response.postalCode),
  prefecture:
    toStringValue(response.prefecture),
  city:
    toStringValue(response.city),
  addressLine1:
    toStringValue(response.addressLine1),
  addressLine2:
    toStringValue(response.addressLine2),

  phone:
    toStringValue(response.phone),
  fax:
    toStringValue(response.fax),
  email:
    toStringValue(response.email),
  websiteUrl:
    toStringValue(response.websiteUrl),

  capitalAmount:
    response.capitalAmount,

  permitNumber:
    toStringValue(response.permitNumber),

  qualifiedInvoiceIssuerNumber:
    toStringValue(
      response.qualifiedInvoiceIssuerNumber,
    ),

  serviceArea:
    toStringValue(response.serviceArea),

  businessContentsText:
    joinLines(response.businessContents),

  certificationInformationText:
    joinLines(
      response.certificationInformation,
    ),

  invoiceBankName:
    toStringValue(response.invoiceBankName),

  invoiceBankBranchName:
    toStringValue(
      response.invoiceBankBranchName,
    ),

  invoiceBankAccountType:
    toStringValue(
      response.invoiceBankAccountType,
    ),

  invoiceBankAccountNumber:
    toStringValue(
      response.invoiceBankAccountNumber,
    ),

  invoiceBankAccountHolder:
    toStringValue(
      response.invoiceBankAccountHolder,
    ),

  invoiceNote:
    toStringValue(response.invoiceNote),

  activeFlag:
    response.activeFlag ?? true,
})

export const toCompanyProfileSaveRequest = (
  form: CompanyProfileForm,
): CompanyProfileSaveRequest => ({
  companyCode:
    form.companyCode.trim() || 'DEFAULT',

  companyName:
    form.companyName.trim(),

  companyNameKana:
    toNullableString(form.companyNameKana),

  shortName:
    toNullableString(form.shortName),

  representativeTitle:
    toNullableString(
      form.representativeTitle,
    ),

  representativeName:
    toNullableString(
      form.representativeName,
    ),

  postalCode:
    toNullableString(form.postalCode),

  prefecture:
    toNullableString(form.prefecture),

  city:
    toNullableString(form.city),

  addressLine1:
    toNullableString(form.addressLine1),

  addressLine2:
    toNullableString(form.addressLine2),

  phone:
    toNullableString(form.phone),

  fax:
    toNullableString(form.fax),

  email:
    toNullableString(form.email),

  websiteUrl:
    toNullableString(form.websiteUrl),

  capitalAmount:
    form.capitalAmount,

  permitNumber:
    toNullableString(form.permitNumber),

  qualifiedInvoiceIssuerNumber:
    toNullableString(
      form.qualifiedInvoiceIssuerNumber,
    ),

  serviceArea:
    toNullableString(form.serviceArea),

  businessContents:
    splitLines(form.businessContentsText),

  certificationInformation:
    splitLines(
      form.certificationInformationText,
    ),

  invoiceBankName:
    toNullableString(form.invoiceBankName),

  invoiceBankBranchName:
    toNullableString(
      form.invoiceBankBranchName,
    ),

  invoiceBankAccountType:
    toNullableString(
      form.invoiceBankAccountType,
    ),

  invoiceBankAccountNumber:
    toNullableString(
      form.invoiceBankAccountNumber,
    ),

  invoiceBankAccountHolder:
    toNullableString(
      form.invoiceBankAccountHolder,
    ),

  invoiceNote:
    toNullableString(form.invoiceNote),

  activeFlag:
    form.activeFlag,
})