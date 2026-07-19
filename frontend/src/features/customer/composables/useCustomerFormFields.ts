import { computed } from 'vue'
import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import type { Customer } from '../types/customerTypes'

export const customerFormTabs = ['基本情報', '契約・請求'] as const

export const useCustomerFormFields = () => {
  const fields = computed<TabbedFormFieldDef<Customer>[]>(() => [
    { key: 'name', label: '顧客名', type: 'text', tab: '基本情報' },
    { key: 'furiganaName', label: 'ふりがな', type: 'text', tab: '基本情報' },
    { key: 'shortName', label: '短縮社名', type: 'text', tab: '基本情報' },
    { key: 'postNo', label: '郵便番号', type: 'text', tab: '基本情報' },
    { key: 'address', label: '住所', type: 'text', tab: '基本情報' },
    { key: 'representativeName', label: '代表者名', type: 'text', tab: '基本情報' },
    { key: 'phone', label: '電話番号', type: 'text', tab: '基本情報' },

    { key: 'jobType', label: '職種', type: 'text', tab: '契約・請求' },
    { key: 'contractFlag', label: '契約有無', type: 'text', tab: '契約・請求' },

    // DayRule
    {
      key: 'closingDayRule',
      label: '締日',
      type: 'dayrule',
      tab: '契約・請求',
    },
    {
      key: 'paymentDayRule',
      label: '支払日',
      type: 'dayrule',
      tab: '契約・請求',
    },
  ])

  return {
    tabs: customerFormTabs,
    fields,
  }
}