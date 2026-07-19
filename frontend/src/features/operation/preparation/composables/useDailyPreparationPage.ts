/* eslint-disable @typescript-eslint/no-explicit-any */
import { computed, ref, watch } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import { useEmployeesQuery } from '@/features/employees/api/useEmployeesQuery'
import { useDailyPreparationQuery } from '../api/useDailyPreparationQuery'
import { useCreateDailyPreparationMutation } from '../api/useCreateDailyPreparationMutation'
import { useBulkSaveDailyPreparationAssignmentsMutation } from '../api/useBulkSaveDailyPreparationAssignmentMutation'
import { useBulkSaveDailyPreparationDispatchesMutation } from '../api/useBulkSaveDailyPreparationDispatchMutation'
import type {
  DailyPreparationAssignmentBulkSaveItemRequest,
  DailyPreparationDispatchBulkSaveItemRequest,
} from '../types/dailyPreparationApiTypes'
import {
  createAssignmentRows,
  type DailyPreparationAssignmentTableRow,
} from './useDailyPreparationAssignmentTableConfig'
import {
  createDispatchRowsFromAssignments,
  type DailyPreparationDispatchTableRow,
} from './useDailyPreparationDispatchTableConfig'
import { useCustomerMasterStore } from '@/features/customer/store/useCustomerMasterStore'

const tomorrow = () => {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

const toNullableNumber = (value: unknown): number | null => {
  if (value == null || value === '') return null

  const num = Number(value)
  return Number.isNaN(num) ? null : num
}

const hasAssignmentValue = (row: DailyPreparationAssignmentTableRow) =>
  row.customerId != null || row.customerSiteId != null || row.workDescription.trim() !== ''

const hasDispatchValue = (row: DailyPreparationDispatchTableRow) =>
  row.customerId != null && row.customerSiteId != null

export const useDailyPreparationPage = () => {
  const targetDate = ref(tomorrow())
  const activeTab = ref<'assignments' | 'dispatches'>('assignments')

  const assignmentRows = ref<DailyPreparationAssignmentTableRow[]>([])
  const dispatchRows = ref<DailyPreparationDispatchTableRow[]>([])

  const employeesQuery = useEmployeesQuery()
  const customerStore = useCustomerMasterStore()

  const preparationQuery = useDailyPreparationQuery(targetDate)
  const createPreparationMutation = useCreateDailyPreparationMutation()
  const bulkSaveAssignmentsMutation = useBulkSaveDailyPreparationAssignmentsMutation()
  const bulkSaveDispatchesMutation = useBulkSaveDailyPreparationDispatchesMutation()

  const preparation = computed(() => preparationQuery.preparation.value)

  const ensurePreparation = async () => {
    if (preparation.value) return preparation.value

    return await createPreparationMutation.mutateAsync({
      targetDate: targetDate.value,
      note: null,
    })
  }

  const getDistanceFromSiteId = (siteId: number | null): number => {
    const site = customerStore.findSite(siteId)
    return site?.distanceFromCompanyKm ?? 0
  }

  const rebuildDispatchRows = () => {
    dispatchRows.value = createDispatchRowsFromAssignments(
      assignmentRows.value,
      preparation.value?.dispatches ?? [],
      dispatchRows.value,
      getDistanceFromSiteId,
    )
  }

  watch(
    () => targetDate.value,
    async () => {
      await customerStore.load()
      rebuildDispatchRows()
    },
    { immediate: true },
  )

  watch(
    () => [employeesQuery.employees.value, preparation.value?.assignments],
    () => {
      assignmentRows.value = createAssignmentRows(
        employeesQuery.employees.value,
        preparation.value?.assignments ?? [],
      )

      rebuildDispatchRows()
    },
    { deep: true, immediate: true },
  )

  watch(
    () => preparation.value?.dispatches,
    () => {
      rebuildDispatchRows()
    },
    { deep: true, immediate: true },
  )

  const markAssignmentRowState = (row: DailyPreparationAssignmentTableRow) => {
    if (row.assignmentId == null) {
      row._isNew = hasAssignmentValue(row)
      row._isUpdated = false
      row._isDeleted = false
      return
    }

    if (!hasAssignmentValue(row)) {
      row._isDeleted = true
      row._isUpdated = false
      row._isNew = false
      return
    }

    row._isUpdated = true
    row._isDeleted = false
    row._isNew = false
  }

  const markDispatchRowState = (row: DailyPreparationDispatchTableRow) => {
    if (row.dispatchId == null) {
      row._isNew = hasDispatchValue(row)
      row._isUpdated = false
      row._isDeleted = false
      return
    }

    if (!hasDispatchValue(row)) {
      row._isDeleted = true
      row._isUpdated = false
      row._isNew = false
      return
    }

    row._isUpdated = true
    row._isDeleted = false
    row._isNew = false
  }

  const handleAssignmentCellUpdate = ({
    id,
    field,
    value,
  }: {
    id: number
    field: keyof DailyPreparationAssignmentTableRow
    value: unknown
  }) => {
    const row = assignmentRows.value.find((item) => item.id === id)
    if (!row) return

    if (field === 'customerId') {
      const customerId = toNullableNumber(value)

      row.customerId = customerId

      const customer = customerStore.findCustomer(customerId)
      row.customerName = customer?.name ?? ''

      row.customerSiteId = null
      row.siteName = ''

      markAssignmentRowState(row)
      rebuildDispatchRows()
      return
    }

    if (field === 'customerSiteId') {
      const siteId = toNullableNumber(value)

      row.customerSiteId = siteId

      const site = customerStore.findSite(siteId)
      row.siteName = site?.name ?? ''

      if (site?.customerId != null) {
        row.customerId = site.customerId

        const customer = customerStore.findCustomer(site.customerId)
        row.customerName = customer?.name ?? ''
      }

      markAssignmentRowState(row)
      rebuildDispatchRows()
      return
    }

    if (field === 'workDescription') {
      row.workDescription = String(value ?? '')

      markAssignmentRowState(row)
      rebuildDispatchRows()
      return
    }

    ;(row[field] as unknown) = value

    markAssignmentRowState(row)
    rebuildDispatchRows()
  }

  const handleDispatchCellUpdate = ({
    id,
    field,
    value,
  }: {
    id: number
    field: keyof DailyPreparationDispatchTableRow
    value: unknown
  }) => {
    const row = dispatchRows.value.find((item) => item.id === id)
    if (!row) return

    if (field === 'vehicleCount') {
      row.vehicleCount = Number(value ?? 0)
      markDispatchRowState(row)
      return
    }

    if (field === 'note') {
      row.note = String(value ?? '')
      markDispatchRowState(row)
      return
    }
  }

  const saveAssignments = async () => {
    const currentPreparation = (await ensurePreparation()) as any

    const items: DailyPreparationAssignmentBulkSaveItemRequest[] = assignmentRows.value
      .filter((row) => row._isNew || row._isUpdated || row._isDeleted)
      .map((row) => ({
        id: row.assignmentId,
        employeeId: row.employeeId,
        customerId: row.customerId,
        customerSiteId: row.customerSiteId,
        workDescription: row.workDescription.trim() || null,
        isNew: row._isNew,
        isUpdated: row._isUpdated,
        isDeleted: row._isDeleted,
      }))

    console.warn('assignmentRows=', assignmentRows.value)
    console.warn('saveItems=', items)

    if (items.length === 0) return

    await bulkSaveAssignmentsMutation.mutateAsync({
      preparationId: currentPreparation.id,
      items,
    })
  }

  const saveDispatches = async () => {
    const currentPreparation = (await ensurePreparation()) as any

    const items: DailyPreparationDispatchBulkSaveItemRequest[] = dispatchRows.value
      .filter((row) => row._isNew || row._isUpdated || row._isDeleted)
      .map((row) => ({
        id: row.dispatchId,
        customerId: row.customerId,
        customerSiteId: row.customerSiteId,
        vehicleCount: row.vehicleCount,
        note: row.note.trim() || null,
        isNew: row._isNew,
        isUpdated: row._isUpdated,
        isDeleted: row._isDeleted,
      }))

    if (items.length === 0) return

    await bulkSaveDispatchesMutation.mutateAsync({
      preparationId: currentPreparation.id,
      items,
    })
  }

  const save = async () => {
    await saveAssignments()
    await saveDispatches()

    await preparationQuery.refetch()
  }

  const hasDirtyRows = computed(
    () =>
      assignmentRows.value.some((row) => row._isNew || row._isUpdated || row._isDeleted) ||
      dispatchRows.value.some((row) => row._isNew || row._isUpdated || row._isDeleted),
  )

  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '保存',
      color: 'success',
      disabled: !hasDirtyRows.value,
      onClick: save,
    },
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '作業伝票出力',
      color: 'secondary',
      disabled: !preparation.value,
      onClick: () => window.alert('作業伝票出力は次で接続します。'),
    },
  ])

  return {
    targetDate,
    activeTab,
    tabs: [
      { label: '従業員配置', value: 'assignments' },
      { label: '現場配車', value: 'dispatches' },
      { label: '帳票', value: 'reports' },
    ],

    employeesQuery,
    preparationQuery,
    preparation,

    assignmentRows,
    dispatchRows,

    leftToolbarItems,
    rightToolbarItems,

    handleAssignmentCellUpdate,
    handleDispatchCellUpdate,
  }
}
