import { computed } from 'vue'
import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import type { ApplicantPersistedRow } from '@/features/application/types/applicantTypes'
import {
  applicantContractTypeOptions,
  applicantGenderOptions,
  applicantRecruitmentStatusOptions,
  applicantRetirementStatusOptions,
  applicantRoomTypeOptions,
  applicantYesNoOptions,
} from '@/features/application/constants/applicantConstants'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'

export const applicantFormTabs = [
  '基本情報',
  '応募情報',
  '媒体情報',
  '属性情報',
  '入退社情報',
] as const

export const useApplicantFormFields = () => {
  const fields = computed<TabbedFormFieldDef<ApplicantPersistedRow>[]>(() => [
    {
      key: 'no',
      label: 'No',
      type: 'number',
      editable: false,
      tab: '基本情報',
    },
    {
      key: 'name',
      label: '氏名',
      type: 'text',
      tab: '基本情報',
    },
    {
      key: 'furiganaName',
      label: 'フリガナ',
      type: 'text',
      tab: '基本情報',
    },
    {
      key: 'contractType',
      label: '雇用形態',
      type: 'select',
      tab: '基本情報',
      options: [...applicantContractTypeOptions],
    },
    {
      key: 'retirementStatus',
      label: 'ステータス',
      type: 'select',
      tab: '基本情報',
      options: [...applicantRetirementStatusOptions],
    },
    {
      key: 'birthDate',
      label: '生年月日',
      type: 'date',
      tab: '基本情報',
    },
    {
      key: 'gender',
      label: '性別',
      type: 'select',
      tab: '基本情報',
      options: [...applicantGenderOptions],
    },

    {
      key: 'contactDate',
      label: '問い合わせ日',
      type: 'date',
      tab: '応募情報',
    },
    {
      key: 'recruitmentStatus',
      label: '採用状況',
      type: 'select',
      tab: '応募情報',
      options: [...applicantRecruitmentStatusOptions],
    },
    {
      key: 'sourceWhere',
      label: 'どこで',
      type: 'text',
      tab: '応募情報',
    },
    {
      key: 'searchSite',
      label: '検索サイト',
      type: 'text',
      tab: '応募情報',
    },
    {
      key: 'keyword',
      label: 'キーワード',
      type: 'text',
      tab: '応募情報',
    },
    {
      key: 'repostedTo',
      label: '転載先',
      type: 'text',
      tab: '応募情報',
    },
    {
      key: 'applicationReason1',
      label: '応募理由1',
      type: 'text',
      tab: '応募情報',
    },
    {
      key: 'applicationReason2',
      label: '応募理由2',
      type: 'text',
      tab: '応募情報',
    },

    {
      key: 'recruitmentCompany',
      label: '求人会社',
      type: 'text',
      tab: '媒体情報',
    },
    {
      key: 'mediaType',
      label: '媒体種類',
      type: 'text',
      tab: '媒体情報',
    },
    {
      key: 'mediaName',
      label: '応募媒体',
      type: 'text',
      tab: '媒体情報',
    },
    {
      key: 'mediaArea',
      label: '応募媒体地域',
      type: 'text',
      tab: '媒体情報',
      editable: false,
    },
    {
      key: 'mediaSlots',
      label: '応募媒体枠数',
      type: 'number',
      tab: '媒体情報',
      editable: false,
    },
    {
      key: 'recruitmentUnitPrice',
      label: '募集単価',
      type: 'number',
      tab: '媒体情報',
      editable: false,
      formatter: (value) => {
        if (value == null || value === '') return ''
        return `${Number(value).toLocaleString()}円`
      },
    },

    {
      key: 'dailyWageAtJoin',
      label: '日給(入社時)',
      type: 'number',
      tab: '属性情報',
      formatter: (value) => {
        if (value == null || value === '') return ''
        return `${Number(value).toLocaleString()}円`
      },
    },
    {
      key: 'needsDormitory',
      label: '入寮有無',
      type: 'select',
      tab: '属性情報',
      options: [...applicantYesNoOptions],
    },
    {
      key: 'roomType',
      label: '部屋タイプ',
      type: 'select',
      tab: '属性情報',
      options: [...applicantRoomTypeOptions],
    },
    {
      key: 'dormitoryFee',
      label: '寮費',
      type: 'number',
      tab: '属性情報',
      formatter: (value) => {
        if (value == null || value === '') return ''
        return `${Number(value).toLocaleString()}円`
      },
    },
    {
      key: 'previousJob',
      label: '前職種',
      type: 'text',
      tab: '属性情報',
    },
    {
      key: 'previousJobPeriod',
      label: '前職在籍期間',
      type: 'text',
      tab: '属性情報',
    },
    {
      key: 'insuredBefore',
      label: '前職社保加入有無',
      type: 'select',
      tab: '属性情報',
      options: [...applicantYesNoOptions],
    },
    {
      key: 'dormitoryExperience',
      label: '社員寮経験有無',
      type: 'select',
      tab: '属性情報',
      options: [...applicantYesNoOptions],
    },
    {
      key: 'previousDormitoryFee',
      label: '前職寮費',
      type: 'number',
      tab: '属性情報',
      formatter: (value, row) => {
        return formatCurrency(value)
      },
    },

    {
      key: 'joinDateWithoutInsurance',
      label: '入社日(社保無)',
      type: 'date',
      tab: '入退社情報',
    },
    {
      key: 'leaveDateWithoutInsurance',
      label: '退職日(社保無)',
      type: 'date',
      tab: '入退社情報',
    },
    {
      key: 'joinDateWithInsurance',
      label: '入社日(社保有)',
      type: 'date',
      tab: '入退社情報',
    },
    {
      key: 'leaveDateWithInsurance',
      label: '退職日(社保有)',
      type: 'date',
      tab: '入退社情報',
    },
    {
      key: 'clientBeforeLeave',
      label: '退職前得意先',
      type: 'text',
      tab: '入退社情報',
    },
    {
      key: 'industryBeforeLeave',
      label: '退職前業種',
      type: 'text',
      tab: '入退社情報',
    },
    {
      key: 'estimatedRetirementReason',
      label: '退職理由（想定）',
      type: 'text',
      tab: '入退社情報',
    },
  ])

  return {
    tabs: applicantFormTabs,
    fields,
  }
}
