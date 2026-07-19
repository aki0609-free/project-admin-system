import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { EmployeeListItemResponse } from '../types/employeeApiTypes'
import {
  employmentStatusOptions,
  employmentTypeOptions,
  toOptionTitle,
} from '../types/employeeEnumTypes'
import { calculateAgeAt, formatAge } from '@/shared/utils/BusinessUtils'
import { formatYearMonthDay } from '@/shared/utils/DateUtils'

export type EmployeeTableRow = SimpleTableEditableRow & {
  id: number
  employeeCode: string
  employeeName: string
  employeeNameKana: string
  employmentType: string
  employmentTypeText: string
  employmentStatus: string
  employmentStatusText: string
  hireDate: string
  hireAge: string
  resignAge: string
  phone: string
  email: string
  activeText: string
  raw: EmployeeListItemResponse
}

export const useEmployeeTableConfig = (employees: Ref<EmployeeListItemResponse[]>) => {
  const rows = computed<EmployeeTableRow[]>(() =>
    employees.value.map((item) => ({
      id: item.id,
      employeeCode: item.employeeCode,
      employeeName: item.employeeName,
      employeeNameKana: item.employeeNameKana ?? '',
      employmentType: item.employmentType,
      employmentTypeText: toOptionTitle(employmentTypeOptions, item.employmentType),
      employmentStatus: item.employmentStatus,
      employmentStatusText: toOptionTitle(employmentStatusOptions, item.employmentStatus),
      hireDate: item.hireDate ?? '',
      hireAge: calculateAgeAt(item.birthDate, item.hireDate),
      resignAge: calculateAgeAt(item.birthDate, item.resignDate),
      phone: item.phone ?? '',
      email: item.email ?? '',
      activeText: item.activeFlag ? '有効' : '無効',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<EmployeeTableRow>[] = [
      { title: 'ID', key: 'id', width: '80px', filter: { type: 'text' } },
      { title: '社員コード', key: 'employeeCode', width: '140px', filter: { type: 'text' } },
      { title: '氏名', key: 'employeeName', width: '180px', filter: { type: 'text' } },
      { title: 'フリガナ', key: 'employeeNameKana', width: '180px', filter: { type: 'text' } },
      { title: '雇用区分', key: 'employmentTypeText', width: '180px', filter: { type: 'text' } },
      { title: '在籍状態', key: 'employmentStatusText', width: '120px', filter: { type: 'text' } },
      { title: '入社日', key: 'hireDate', width: '180px', filter: { type: 'text' }, formatter: (value) => formatYearMonthDay(value as string) },
      { title: '入社時年齢', key: 'hireAge', width: '140px', filter: { type: 'text' }, formatter: formatAge, },
      { title: '退社時年齢', key: 'resignAge', width: '140px', filter: { type: 'text' }, formatter: formatAge, },
      { title: '電話', key: 'phone', width: '150px', filter: { type: 'text' } },
      { title: 'メール', key: 'email', width: '220px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', width: '100px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() => createSimpleTableFilterRules<EmployeeTableRow>(columns.value))

  return {
    rows,
    columns,
    filterRules,
  }
}
