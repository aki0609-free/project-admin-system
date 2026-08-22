import { computed, ref, watch } from 'vue'
import { useCreateCustomerMutation } from '../api/useCreateCustomerMutation'
import { useCustomerDetailQuery } from '../api/useCustomerDetailQuery'
import { useDeleteCustomerMutation } from '../api/useDeleteCustomerMutation'
import { useUpdateCustomerMutation } from '../api/useUpdateCustomerMutation'
import {
  toCustomer,
  toCustomerEmployee,
  toCustomerSaveRequest,
  toCustomerSite,
} from '../mapper/customerMapper'
import type {
  Customer,
  CustomerListItem,
  CustomerSavePayload,
} from '../types/customerTypes'

function createEmptyCustomer(): Customer {
  return {
    id: -Date.now(),
    name: '',
    furiganaName: '',
    shortName: '',
    postNo: '',
    address: '',
    representativeName: '',
    phone: '',
    jobType: '',
    contractFlag: '',
    invoiceType: 'PATTERN_1',
    closingDayRule: null,
    paymentDayRule: null,
  }
}

export const useCustomerEditDialog = () => {
  const dialog = ref(false)
  const isCreateMode = ref(false)
  const selectedCustomerId = ref<number | null>(null)
  const editingCustomer = ref<Customer | null>(null)
  const errorMessage = ref('')

  const customerDetailQuery = useCustomerDetailQuery(selectedCustomerId)

  const createMutation = useCreateCustomerMutation()
  const updateMutation = useUpdateCustomerMutation()
  const deleteMutation = useDeleteCustomerMutation()

  const loading = computed(
    () =>
      customerDetailQuery.isFetching.value ||
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  const detail = computed(() => customerDetailQuery.data.value ?? null)

  watch(detail, (value) => {
    if (!value || isCreateMode.value || value.id !== selectedCustomerId.value) return
    editingCustomer.value = toCustomer(value)
  })

  const editingSites = computed(() =>
    detail.value?.sites.map(toCustomerSite) ?? [],
  )

  const editingEmployees = computed(() =>
    detail.value?.employees.map(toCustomerEmployee) ?? [],
  )

  function openCreate() {
    errorMessage.value = ''
    isCreateMode.value = true
    selectedCustomerId.value = null
    editingCustomer.value = createEmptyCustomer()
    dialog.value = true
  }

  function openEdit(row: CustomerListItem) {
    errorMessage.value = ''
    isCreateMode.value = false
    selectedCustomerId.value = row.id
    editingCustomer.value = toCustomer(row)
    dialog.value = true
  }

  async function save(payload: CustomerSavePayload) {
    try {
      errorMessage.value = ''
      const body = toCustomerSaveRequest(payload)

      if (isCreateMode.value) {
        await createMutation.mutateAsync(body)
      } else {
        await updateMutation.mutateAsync({
          id: payload.customer.id,
          body,
        })
      }

      dialog.value = false
      selectedCustomerId.value = null
      editingCustomer.value = null
    } catch (error) {
      errorMessage.value = toErrorMessage(error, '顧客情報の保存に失敗しました。')
    }
  }

  async function remove(id: number) {
    try {
      errorMessage.value = ''
      await deleteMutation.mutateAsync(id)

      dialog.value = false
      selectedCustomerId.value = null
      editingCustomer.value = null
    } catch (error) {
      errorMessage.value = toErrorMessage(error, '顧客情報の削除に失敗しました。')
    }
  }

  function clearError() {
    errorMessage.value = ''
  }

  function toErrorMessage(error: unknown, fallback: string): string {
    if (error instanceof Error && error.message) return error.message
    if (typeof error === 'object' && error !== null) {
      const candidate = error as Record<string, unknown>
      if (typeof candidate.message === 'string') return candidate.message
    }
    return fallback
  }

  return {
    dialog,
    isCreateMode,
    selectedCustomerId,
    editingCustomer,
    editingSites,
    editingEmployees,
    loading,
    errorMessage,
    openCreate,
    openEdit,
    save,
    remove,
    clearError,
  }
}
