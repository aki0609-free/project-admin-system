import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  applicantContractTypeLabelMap,
  applicantContractTypeOptions,
  applicantGenderLabelMap,
  applicantGenderOptions,
  applicantRetirementStatusLabelMap,
  applicantRetirementStatusOptions,
} from '@/features/application/constants/applicantConstants'
import { calculateAgeAtDate, formatYearMonthDay } from '@/shared/utils/DateUtils'
import { formatCurrentTenure } from '@/shared/utils/TenureUtils'

export const useApplicantBasicColumns = () => {
  const columns = computed<SimpleTableColumnDef<ApplicantRow>[]>(() => [
    {
      title: 'No',
      key: 'no',
      filter: { type: 'text' },
      width: '180px',
    },
    {
      title: '氏名',
      key: 'name',
      filter: { type: 'text' },
      width: '180px',
    },
    {
      title: 'フリガナ',
      key: 'furiganaName',
      filter: { type: 'text' },
      width: '180px',
    },
    {
      title: '雇用形態',
      key: 'contractType',
      filter: { type: 'select' },
      width: '180px',
      enumOptions: [...applicantContractTypeOptions],
      formatter: value =>
        applicantContractTypeLabelMap[
          value as keyof typeof applicantContractTypeLabelMap
        ] ?? '',
    },
    {
      title: 'ステータス',
      key: 'retirementStatus',
      filter: { type: 'select' },
      width: '180px',
      enumOptions: [...applicantRetirementStatusOptions],
      formatter: (value) =>
        applicantRetirementStatusLabelMap[
          value as keyof typeof applicantRetirementStatusLabelMap
        ] ?? '',
    },
    {
      title: '現時点での在籍期間',
      key: 'currentTenure',
      width: '180px',
      filter: { type: 'text' },
      computed: true,
      valueGetter: (row) =>
        formatCurrentTenure(
          row.joinDateWithInsurance || row.joinDateWithoutInsurance,
          row.leaveDateWithInsurance || row.leaveDateWithoutInsurance,
        )
    },
    {
      title: '生年月日',
      key: 'birthDate',
      width: '180px',
      filter: { type: 'date' },
      formatter: value => formatYearMonthDay(value as string)
    },
    {
      title: '年齢（入社時）',
      key: 'ageAtJoin',
      filter: { type: 'text' },
      computed: true,
      width: '180px',
      valueGetter: (row) => calculateAgeAtDate(row.birthDate, row.joinDateWithInsurance),
      formatter: (value, row) => !!value ? `${value}歳` : '',
    },
    {
      title: '年齢（退社時）',
      key: 'ageAtLeave',
      filter: { type: 'text' },
      computed: true,
      width: '180px',
      valueGetter: (row) => calculateAgeAtDate(row.birthDate, row.leaveDateWithInsurance),
      formatter: (value, row) => !!value ? `${value}歳` : '',
    },
    {
      title: '性別',
      key: 'gender',
      filter: { type: 'select' },
      width: '180px',
      enumOptions: [...applicantGenderOptions],
      formatter: value =>
        applicantGenderLabelMap[
          value as keyof typeof applicantGenderLabelMap
        ] ?? '',
    },
  ])

  return {
    columns,
  }
}