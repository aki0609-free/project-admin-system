import { computed } from 'vue'
import type { SimpleTableColumnDef } from '@/shared/components/table/simple_table/types/item/types'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import {
  applicantRoomTypeLabelMap,
  applicantRoomTypeOptions,
  applicantYesNoLabelMap,
  applicantYesNoOptions,
} from '@/features/application/constants/applicantConstants'

export const useApplicantProfileColumns = () => {
  const columns = computed<SimpleTableColumnDef<ApplicantRow>[]>(() => [
    {
      title: 'No',
      key: 'no',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '氏名',
      key: 'name',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '日給(入社時)',
      key: 'dailyWageAtJoin',
      width: '180px',
      filter: { type: 'text' },
      formatter: value =>
        value == null || value === '' ? '' : `${Number(value).toLocaleString()}円`,
    },
    {
      title: '入寮有無',
      key: 'needsDormitory',
      width: '180px',
      filter: { type: 'select' },
      enumOptions: [...applicantYesNoOptions],
      formatter: value =>
        applicantYesNoLabelMap[value as keyof typeof applicantYesNoLabelMap] ?? '',
    },
    {
      title: '部屋タイプ',
      key: 'roomType',
      width: '180px',
      filter: { type: 'select' },
      enumOptions: [...applicantRoomTypeOptions],
      formatter: value =>
        applicantRoomTypeLabelMap[
          value as keyof typeof applicantRoomTypeLabelMap
        ] ?? '',
    },
    {
      title: '寮費',
      key: 'dormitoryFee',
      width: '180px',
      filter: { type: 'text' },
      formatter: value =>
        value == null || value === '' ? '' : `${Number(value).toLocaleString()}円`,
    },
    {
      title: '前職種',
      key: 'previousJob',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '前職在籍期間',
      key: 'previousJobPeriod',
      width: '180px',
      filter: { type: 'text' },
    },
    {
      title: '前職社保加入有無',
      key: 'insuredBefore',
      filter: { type: 'select' },
      width: '180px',
      enumOptions: [...applicantYesNoOptions],
      formatter: value =>
        applicantYesNoLabelMap[value as keyof typeof applicantYesNoLabelMap] ?? '',
    },
    {
      title: '社員寮経験有無',
      key: 'dormitoryExperience',
      width: '180px',
      filter: { type: 'select' },
      enumOptions: [...applicantYesNoOptions],
      formatter: value =>
        applicantYesNoLabelMap[value as keyof typeof applicantYesNoLabelMap] ?? '',
    },
    {
      title: '前職寮費',
      key: 'previousDormitoryFee',
      width: '180px',
      filter: { type: 'text' },
      formatter: value =>
        value == null || value === '' ? '' : `${Number(value).toLocaleString()}円`,
    },
  ])

  return {
    columns,
  }
}