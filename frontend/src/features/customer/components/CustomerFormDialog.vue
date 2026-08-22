<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'

import CustomerBillingRateTab from './CustomerBillingRateTab.vue'

import { useCustomerEmployeeColumns } from '../composables/useCustomerEmployeeColumns'
import { useCustomerFormFields } from '../composables/useCustomerFormFields'
import { useCustomerSiteColumns } from '../composables/useCustomerSiteColumns'
import { useEditableChildRows } from '../composables/useEditableChildRows'

import type {
  Customer,
  CustomerEmployee,
  CustomerSavePayload,
  CustomerSite,
} from '../types/customerTypes'
import { customerSchema } from '../validation/customerSchema'

const props = defineProps<{
  modelValue: boolean
  customer: Customer | null
  sites: CustomerSite[]
  employees: CustomerEmployee[]
  isCreateMode: boolean
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void

  (e: 'save', value: CustomerSavePayload): void

  (e: 'delete', id: number): void

  (e: 'dismiss-error'): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const activeTab = ref('basic')
const validationMessage = ref('')
const basicFormRef = ref<{ validateAll: () => boolean } | null>(null)

const pageTabs = [
  {
    label: '基本情報',
    value: 'basic',
  },
  {
    label: '現場一覧',
    value: 'sites',
  },
  {
    label: '顧客社員',
    value: 'employees',
  },
  {
    label: '請求単価',
    value: 'billingRates',
  },
]

const form = reactive<Customer>({
  id: -1,
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
})

const siteRows = useEditableChildRows<CustomerSite>()

const employeeRows = useEditableChildRows<CustomerEmployee>()

const customerId = computed<number | null>(() => {
  if (props.isCreateMode || form.id <= 0) {
    return null
  }

  return form.id
})

watch(
  () => props.modelValue,
  (opened) => {
    if (!opened) {
      return
    }

    activeTab.value = 'basic'
    validationMessage.value = ''
  },
)

watch(
  () => props.customer,
  (value) => {
    if (!value) {
      return
    }

    Object.assign(form, value)
  },
  {
    immediate: true,
  },
)

watch(
  () => props.sites,
  (value) => {
    siteRows.resetRows(value)
  },
  {
    immediate: true,
  },
)

watch(
  () => props.employees,
  (value) => {
    employeeRows.resetRows(value)
  },
  {
    immediate: true,
  },
)

const { tabs: formTabs, fields } = useCustomerFormFields()

const { columns: siteColumns } = useCustomerSiteColumns()

const { columns: employeeColumns } = useCustomerEmployeeColumns()

const siteFilterRules = computed(() =>
  createSimpleTableFilterRules<CustomerSite>(siteColumns.value),
)

const employeeFilterRules = computed(() =>
  createSimpleTableFilterRules<CustomerEmployee>(employeeColumns.value),
)

const schema = customerSchema

function handleClose() {
  dialogModel.value = false
}

function dismissError() {
  validationMessage.value = ''
  emit('dismiss-error')
}

async function handleSave() {
  validationMessage.value = ''

  const result = schema.safeParse(form)
  if (!result.success) {
    activeTab.value = 'basic'
    validationMessage.value = result.error.issues[0]?.message ?? '基本情報を確認してください。'
    await nextTick()
    basicFormRef.value?.validateAll()
    return
  }

  const childError = validateChildRows()
  if (childError) {
    activeTab.value = childError.tab
    validationMessage.value = childError.message
    return
  }

  emit('save', {
    customer: {
      ...form,
    },

    sites: [...siteRows.rows.value],

    employees: [...employeeRows.rows.value],
  })
}

function validateChildRows(): { tab: string; message: string } | null {
  const sites = siteRows.visibleRows.value
  for (const site of sites) {
    if (!site.name.trim()) {
      return { tab: 'sites', message: '現場名は必須です。' }
    }
    if (site.contactPersonEmail && !isEmail(site.contactPersonEmail)) {
      return { tab: 'sites', message: `現場「${site.name}」のメールアドレス形式が正しくありません。` }
    }
    if (site.distanceFromCompanyKm != null) {
      const distance = Number(site.distanceFromCompanyKm)
      if (!Number.isFinite(distance) || distance < 0 || !Number.isInteger(distance)) {
        return { tab: 'sites', message: `現場「${site.name}」の会社からの距離は0以上の整数で入力してください。` }
      }
    }
  }

  const employees = employeeRows.visibleRows.value
  for (const employee of employees) {
    if (!employee.name.trim()) {
      return { tab: 'employees', message: '顧客社員名は必須です。' }
    }
    if (employee.email && !isEmail(employee.email)) {
      return { tab: 'employees', message: `顧客社員「${employee.name}」のメールアドレス形式が正しくありません。` }
    }
    if ((employee.invoiceToFlag || employee.invoiceCcFlag) && !employee.email.trim()) {
      return { tab: 'employees', message: `請求書送付先に指定した「${employee.name}」にはメールアドレスが必要です。` }
    }
  }

  return null
}

function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
}

function handleDelete() {
  if (props.isCreateMode) {
    return
  }

  const confirmed = window.confirm(`顧客「${form.name}」を削除しますか？`)

  if (!confirmed) {
    return
  }

  emit('delete', form.id)
}

function addSite() {
  siteRows.addRow({
    id: -Date.now(),
    customerId: form.id,

    deleteSelected: false,

    name: '',
    contactPersonName: '',
    contactPersonPhone: '',
    contactPersonEmail: '',
    distanceFromCompanyKm: null,

    _isNew: true,
    _isUpdated: false,
    _isDeleted: false,
  })
}

function addEmployee() {
  employeeRows.addRow({
    id: -Date.now(),
    customerId: form.id,

    deleteSelected: false,

    name: '',
    furiganaName: '',
    position: '',
    phone: '',
    email: '',

    invoiceToFlag: false,
    invoiceCcFlag: false,

    _isNew: true,
    _isUpdated: false,
    _isDeleted: false,
  })
}

function handleBillingRateSaved() {
  // 請求単価タブ側で再取得済み。
  // 顧客画面側で追加処理が必要になった場合の拡張用。
}

const leftFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '顧客削除',
    color: 'error',
    intent: 'danger',
    visible: !props.isCreateMode,
    onClick: handleDelete,
  },
])

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: 'キャンセル',
    intent: 'utility',
    disabled: props.loading,
    onClick: handleClose,
  },
  {
    type: 'button',
    label: '顧客情報を保存',
    color: 'primary',
    intent: 'primary',
    loading: props.loading,
    disabled: props.loading,
    onClick: () => void handleSave(),
  },
])

const siteToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '現場追加',
    color: 'primary',
    intent: 'primary',
    onClick: addSite,
  },
  {
    type: 'button',
    label: '選択行を削除',
    color: 'error',
    intent: 'danger',
    disabled: !siteRows.hasDeleteSelected.value,
    onClick: siteRows.markSelectedDeleted,
  },
])

const employeeToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '顧客社員追加',
    color: 'primary',
    intent: 'primary',
    onClick: addEmployee,
  },
  {
    type: 'button',
    label: '選択行を削除',
    color: 'error',
    intent: 'danger',
    disabled: !employeeRows.hasDeleteSelected.value,
    onClick: employeeRows.markSelectedDeleted,
  },
])
</script>

<template>
  <AppDialog
    v-model="dialogModel"
    :title="isCreateMode ? '顧客 新規登録' : '顧客 編集'"
    size="xl"
    :max-width="1600"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <v-progress-linear v-if="loading" indeterminate class="mb-3" />

    <v-alert
      v-if="validationMessage || errorMessage"
      type="error"
      variant="tonal"
      closable
      class="mb-3"
      @click:close="dismissError"
    >
      {{ validationMessage || errorMessage }}
    </v-alert>

    <TabLayout v-model="activeTab" :tabs="pageTabs">
      <template #default="{ active }">
        <div v-if="active === 'basic'">
          <FormLayout ref="basicFormRef" v-model="form" :schema="schema">
            <TabbedForm v-model="form" :tabs="[...formTabs]" :fields="fields" />
          </FormLayout>
        </div>

        <div v-else-if="active === 'sites'">
          <AppToolbar :left-items="siteToolbarItems" surface="plain" />

          <div class="table-wrapper">
            <SimpleTable
              table-key="customer-sites"
              item-key="id"
              :items="siteRows.visibleRows.value"
              :columns="siteColumns"
              :filter-rules="siteFilterRules"
              @update:items="siteRows.updateCell"
            />
          </div>
        </div>

        <div v-else-if="active === 'employees'">
          <AppToolbar :left-items="employeeToolbarItems" surface="plain" />

          <div class="table-wrapper">
            <SimpleTable
              table-key="customer-employees"
              item-key="id"
              :items="employeeRows.visibleRows.value"
              :columns="employeeColumns"
              :filter-rules="employeeFilterRules"
              @update:items="employeeRows.updateCell"
            />
          </div>
        </div>

        <CustomerBillingRateTab
          v-else-if="active === 'billingRates'"
          :customer-id="customerId"
          :sites="siteRows.rows.value"
          :is-create-mode="isCreateMode"
          @saved="handleBillingRateSaved"
        />
      </template>
    </TabLayout>
  </AppDialog>
</template>

<style scoped>
.table-wrapper {
  max-width: 100%;
  overflow: auto;
}
</style>
