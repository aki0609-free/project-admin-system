import { computed, ref } from 'vue'
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
    closingDayRule: null,
    paymentDayRule: null,
  }
}

export const useCustomerEditDialog = () => {
  const dialog = ref(false)
  const isCreateMode = ref(false)
  const selectedCustomerId = ref<number | null>(null)
  const editingCustomer = ref<Customer | null>(null)

  const customerDetailQuery = useCustomerDetailQuery(selectedCustomerId)

  const createMutation = useCreateCustomerMutation()
  const updateMutation = useUpdateCustomerMutation()
  const deleteMutation = useDeleteCustomerMutation()

  const detail = computed(() => customerDetailQuery.data.value ?? null)

  const editingSites = computed(() =>
    detail.value?.sites.map(toCustomerSite) ?? [],
  )

  const editingEmployees = computed(() =>
    detail.value?.employees.map(toCustomerEmployee) ?? [],
  )

  function openCreate() {
    isCreateMode.value = true
    selectedCustomerId.value = null
    editingCustomer.value = createEmptyCustomer()
    dialog.value = true
  }

  function openEdit(row: CustomerListItem) {
    isCreateMode.value = false
    selectedCustomerId.value = row.id
    editingCustomer.value = toCustomer(row)
    dialog.value = true
  }

  async function save(payload: CustomerSavePayload) {
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
  }

  async function remove(id: number) {
    await deleteMutation.mutateAsync(id)

    dialog.value = false
    selectedCustomerId.value = null
    editingCustomer.value = null
  }

  return {
    dialog,
    isCreateMode,
    selectedCustomerId,
    editingCustomer,
    editingSites,
    editingEmployees,
    openCreate,
    openEdit,
    save,
    remove,
  }
}