import { computed, reactive, ref, watch, type Ref } from 'vue'
import { z } from 'zod'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { EmployeeDetailResponse } from '../types/employeeApiTypes'
import type {
  EmployeeContractForm,
  EmployeeForm,
  EmployeePayrollProfileForm,
} from '../types/employeeFormTypes'
import { createEmptyEmployeeForm, toEmployeeForm } from '../utils/employeeFormFactory'
import { formatZipCode } from '@/shared/utils/BusinessUtils'

export const employeeBasicSchema = z.object({
  id: z.number(),
  employeeCode: z.string().min(1, '必須です'),
  employeeName: z.string().min(1, '必須です'),
  employeeNameKana: z.string(),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']).nullable(),
  birthDate: z.string(),
  hireDate: z.string(),
  resignDate: z.string(),
  employmentType: z.enum(['FULL_TIME', 'CONTRACT', 'PART_TIME', 'TEMPORARY', 'DAILY_WORKER']),
  employmentStatus: z.enum(['ACTIVE', 'LEAVE', 'RESIGNED']),
  phone: z.string(),
  email: z.string(),
  postalCode: z.string(),
  address: z.string(),
  activeFlag: z.boolean(),
  payrollProfile: z.any(),
  contract: z.any(),
})

export const employeePayrollSchema = z.object({
  taxCategory: z.enum(['KOU', 'OTSU', 'HEI']),
  taxDependentCount: z.number().min(0),
  dependentFlag: z.boolean(),
  dependentOfOtherFlag: z.boolean(),
  paidLeaveRemainingDays: z.number().min(0),
  incomeTaxCalcFlag: z.boolean(),
  residentTaxCalcFlag: z.boolean(),
  residentTaxMonthly: z.number().min(0),
  employmentInsuranceFlag: z.boolean(),
  socialInsuranceFlag: z.boolean(),
  healthInsuranceFlag: z.boolean(),
  pensionInsuranceFlag: z.boolean(),
  careInsuranceFlag: z.boolean(),
  dailyPayFlag: z.boolean(),
  commuteAllowanceMonthly: z.number().min(0),
})

export const employeeContractSchema = z.object({
  contractStartDate: z.string(),
  contractEndDate: z.string(),
  renewalFlag: z.boolean(),
  salaryType: z.enum(['MONTHLY', 'WEEKLY', 'DAILY', 'HOURLY']),
  paymentCycle: z.enum(['DAILY', 'WEEKLY', 'MONTHLY']),
  monthlySalary: z.number().min(0),
  weeklyWage: z.number().min(0),
  dailyWage: z.number().min(0),
  hourlyWage: z.number().min(0),
  standardWorkingHours: z.number().min(0),
  note: z.string(),
})

export const useEmployeeEditDialog = (
  visible: Ref<boolean>,
  employee: Ref<EmployeeDetailResponse | null>,
  emitSave: (form: EmployeeForm) => void,
  emitDelete: (form: EmployeeForm) => void,
) => {
  const activeTab = ref<'basic' | 'payroll' | 'contract'>('basic')
  const formModel = reactive<EmployeeForm>(createEmptyEmployeeForm())
  const resignDialogVisible = ref(false)

  const resetForm = () => {
    Object.assign(formModel, createEmptyEmployeeForm())
    activeTab.value = 'basic'
    resignDialogVisible.value = false
  }

  watch(
    () => visible.value,
    (opened) => {
      if (!opened) return

      if (!employee.value) {
        resetForm()
      }
    },
    { immediate: true },
  )

  watch(
    () => employee.value,
    (value) => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      Object.assign(formModel, toEmployeeForm(value))
      activeTab.value = 'basic'
      resignDialogVisible.value = false
    },
    { immediate: true },
  )

  const isEdit = computed(() => formModel.id > 0)

  const basicFields: GridFormFieldDef<EmployeeForm>[] = [
    { key: 'employeeCode', label: '社員コード', type: 'text', gridColumn: '1 / span 4' },
    { key: 'employeeName', label: '氏名', type: 'text', gridColumn: '1 / span 2' },
    { key: 'employeeNameKana', label: 'フリガナ', type: 'text', gridColumn: '3 / span 2' },
    {
      key: 'gender',
      label: '性別',
      type: 'select',
      options: [
        { title: '男性', value: 'MALE' },
        { title: '女性', value: 'FEMALE' },
        { title: 'その他', value: 'OTHER' },
      ],
      gridColumn: '1 / span 2',
    },
    { key: 'birthDate', label: '生年月日', type: 'date', gridColumn: '3 / span 2' },
    { key: 'hireDate', label: '入社日', type: 'date', gridColumn: '1 / span 2' },
    { key: 'resignDate', label: '退職日', type: 'date', gridColumn: '3 / span 2' },
    {
      key: 'employmentType',
      label: '雇用区分',
      type: 'select',
      options: [
        { title: '正社員', value: 'FULL_TIME' },
        { title: '契約社員', value: 'CONTRACT' },
        { title: 'パート', value: 'PART_TIME' },
        { title: '派遣', value: 'TEMPORARY' },
        { title: '日雇い', value: 'DAILY_WORKER' },
      ],
    },
    {
      key: 'employmentStatus',
      label: '在籍状態',
      type: 'select',
      options: [
        { title: '在籍', value: 'ACTIVE' },
        { title: '休職', value: 'LEAVE' },
        { title: '退職', value: 'RESIGNED' },
      ],
    },
    { key: 'phone', label: '電話', type: 'text', gridColumn: '3 / span 2' },
    { key: 'email', label: 'メール', type: 'text', gridColumn: '1 / span 4' },
    {
      key: 'postalCode',
      label: '郵便番号',
      type: 'text',
      formatter: (value) => formatZipCode(value as string),
    },
    { key: 'address', label: '住所', type: 'text', gridColumn: '2 / span 3' },
    { key: 'activeFlag', label: '有効', type: 'checkbox', width: 100 },
  ]

  const payrollFields: GridFormFieldDef<EmployeePayrollProfileForm>[] = [
    {
      key: 'taxCategory',
      label: '税区分',
      type: 'select',
      options: [
        { title: '甲', value: 'KOU' },
        { title: '乙', value: 'OTSU' },
        { title: '丙', value: 'HEI' },
      ],
    },
    { key: 'taxDependentCount', label: '扶養人数', type: 'number' },
    { key: 'dependentFlag', label: '扶養者あり', type: 'checkbox', width: 120 },
    { key: 'dependentOfOtherFlag', label: '被扶養者', type: 'checkbox', width: 120 },
    { key: 'paidLeaveRemainingDays', label: '有給残日数', type: 'number' },
    { key: 'residentTaxMonthly', label: '住民税月額', type: 'number' },
    { key: 'commuteAllowanceMonthly', label: '通勤手当月額', type: 'number' },
    { key: 'incomeTaxCalcFlag', label: '所得税計算', type: 'checkbox', width: 120 },
    { key: 'residentTaxCalcFlag', label: '住民税控除', type: 'checkbox', width: 120 },
    { key: 'employmentInsuranceFlag', label: '雇用保険', type: 'checkbox', width: 120 },
    { key: 'socialInsuranceFlag', label: '社保対象', type: 'checkbox', width: 120 },
    { key: 'healthInsuranceFlag', label: '健康保険', type: 'checkbox', width: 120 },
    { key: 'pensionInsuranceFlag', label: '厚生年金', type: 'checkbox', width: 120 },
    { key: 'careInsuranceFlag', label: '介護保険', type: 'checkbox', width: 120 },
    { key: 'dailyPayFlag', label: '日払い対象', type: 'checkbox', width: 120 },
  ]

  const contractFields: GridFormFieldDef<EmployeeContractForm>[] = [
    { key: 'contractStartDate', label: '契約開始日', type: 'date' },
    { key: 'contractEndDate', label: '契約終了日', type: 'date' },
    { key: 'renewalFlag', label: '更新あり', type: 'checkbox', width: 120 },
    {
      key: 'salaryType',
      label: '給与形態',
      type: 'select',
      options: [
        { title: '月給', value: 'MONTHLY' },
        { title: '週給', value: 'WEEKLY' },
        { title: '日給', value: 'DAILY' },
        { title: '時給', value: 'HOURLY' },
      ],
    },
    {
      key: 'paymentCycle',
      label: '支払サイクル',
      type: 'select',
      options: [
        { title: '日払い', value: 'DAILY' },
        { title: '週払い', value: 'WEEKLY' },
        { title: '月払い', value: 'MONTHLY' },
      ],
    },
    { key: 'monthlySalary', label: '月給', type: 'number' },
    { key: 'weeklyWage', label: '週給', type: 'number' },
    { key: 'dailyWage', label: '日給', type: 'number' },
    { key: 'hourlyWage', label: '時給', type: 'number' },
    { key: 'standardWorkingHours', label: '標準労働時間', type: 'number' },
  ]

  const tabs = [
    { label: '基本情報', value: 'basic' },
    { label: '給与・税金', value: 'payroll' },
    { label: '契約情報', value: 'contract' },
  ]

  const close = () => {
    visible.value = false
  }

  const save = () => {
    emitSave({
      ...formModel,
      payrollProfile: { ...formModel.payrollProfile },
      contract: { ...formModel.contract },
    })
  }

  const remove = () => {
    emitDelete({
      ...formModel,
      payrollProfile: { ...formModel.payrollProfile },
      contract: { ...formModel.contract },
    })
  }

  const openResignDialog = () => {
    resignDialogVisible.value = true
  }

  const footerItems = computed<ToolbarItem[]>(() => {
    const items: ToolbarItem[] = []

    if (isEdit.value) {
      items.push({
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: remove,
      })
    }

    if (isEdit.value && formModel.activeFlag && formModel.employmentStatus !== 'RESIGNED') {
      items.push({
        type: 'button',
        label: '退職',
        color: 'warning',
        onClick: openResignDialog,
      })
    }

    items.push(
      {
        type: 'button',
        label: '閉じる',
        color: 'secondary',
        onClick: close,
      },
      {
        type: 'button',
        label: '保存',
        color: 'primary',
        onClick: save,
      },
    )

    return items
  })

  return {
    activeTab,
    formModel,
    isEdit,
    tabs,
    basicFields,
    payrollFields,
    contractFields,
    basicSchema: employeeBasicSchema,
    payrollSchema: employeePayrollSchema,
    contractSchema: employeeContractSchema,
    footerItems,
    resignDialogVisible,
  }
}
