import { computed } from 'vue'

import type {
  GridFormFieldDef,
} from '@/shared/components/form/grid_based_form/types/types'

import type {
  CompanyProfileForm,
} from '../types/companyProfileTypes'

export const useCompanyProfileFormFields = () => {
  const tabs = [
    {
      label: '会社情報',
      value: 'basic',
    },
    {
      label: '請求書設定',
      value: 'invoice',
    },
    {
      label: '許認可・事業内容',
      value: 'certification',
    },
  ]

  const basicFields = computed<
    GridFormFieldDef<CompanyProfileForm>[]
  >(() => [
    {
      key: 'companyCode',
      label: '会社コード',
      type: 'text',
      gridColumn: '1 / span 1',
    },
    {
      key: 'companyName',
      label: '会社名',
      type: 'text',
      gridColumn: '2 / span 2',
    },
    {
      key: 'shortName',
      label: '略称',
      type: 'text',
      gridColumn: '4 / span 1',
    },
    {
      key: 'companyNameKana',
      label: '会社名カナ',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'capitalAmount',
      label: '資本金',
      type: 'number',
      gridColumn: '3 / span 2',
    },
    {
      key: 'representativeTitle',
      label: '代表者役職',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'representativeName',
      label: '代表者名',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'postalCode',
      label: '郵便番号',
      type: 'text',
      gridColumn: '1 / span 1',
    },
    {
      key: 'prefecture',
      label: '都道府県',
      type: 'text',
      gridColumn: '2 / span 1',
    },
    {
      key: 'city',
      label: '市区町村',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'addressLine1',
      label: '住所1',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'addressLine2',
      label: '住所2',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'phone',
      label: '電話番号',
      type: 'text',
      gridColumn: '1 / span 1',
    },
    {
      key: 'fax',
      label: 'FAX',
      type: 'text',
      gridColumn: '2 / span 1',
    },
    {
      key: 'email',
      label: 'メールアドレス',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'websiteUrl',
      label: 'Webサイト',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'serviceArea',
      label: '対応エリア',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'activeFlag',
      label: '有効',
      type: 'checkbox',
      gridColumn: '1 / span 1',
    },
  ])

  const invoiceFields = computed<
    GridFormFieldDef<CompanyProfileForm>[]
  >(() => [
    {
      key: 'qualifiedInvoiceIssuerNumber',
      label: '適格請求書発行事業者登録番号',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'invoiceBankName',
      label: '銀行名',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'invoiceBankBranchName',
      label: '支店名',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'invoiceBankAccountType',
      label: '口座種別',
      type: 'select',
      gridColumn: '1 / span 1',
      options: [
        {
          title: '普通',
          value: '普通',
        },
        {
          title: '当座',
          value: '当座',
        },
        {
          title: '貯蓄',
          value: '貯蓄',
        },
      ],
    },
    {
      key: 'invoiceBankAccountNumber',
      label: '口座番号',
      type: 'text',
      gridColumn: '2 / span 1',
    },
    {
      key: 'invoiceBankAccountHolder',
      label: '口座名義',
      type: 'text',
      gridColumn: '3 / span 2',
    },
  ])

  const certificationFields = computed<
    GridFormFieldDef<CompanyProfileForm>[]
  >(() => [
    {
      key: 'permitNumber',
      label: '代表許可番号',
      type: 'text',
      gridColumn: '1 / span 4',
    },
  ])

  return {
    tabs,
    basicFields,
    invoiceFields,
    certificationFields,
  }
}