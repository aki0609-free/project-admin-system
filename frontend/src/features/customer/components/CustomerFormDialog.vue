<script setup lang="ts">
import {
  computed,
  reactive,
  ref,
  watch,
} from 'vue'

import { z } from 'zod'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import TabbedForm from '@/shared/components/form/tabbed_form/TabbedForm.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'

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

const props = defineProps<{
  modelValue: boolean
  customer: Customer | null
  sites: CustomerSite[]
  employees: CustomerEmployee[]
  isCreateMode: boolean
}>()

const emit = defineEmits<{
  (
    e: 'update:modelValue',
    value: boolean,
  ): void

  (
    e: 'save',
    value: CustomerSavePayload,
  ): void

  (
    e: 'delete',
    id: number,
  ): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: value =>
    emit('update:modelValue', value),
})

const activeTab = ref('basic')

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
  closingDayRule: null,
  paymentDayRule: null,
})

const siteRows =
  useEditableChildRows<CustomerSite>()

const employeeRows =
  useEditableChildRows<CustomerEmployee>()

const customerId = computed<number | null>(() => {
  if (
    props.isCreateMode
    || form.id <= 0
  ) {
    return null
  }

  return form.id
})

watch(
  () => props.modelValue,
  opened => {
    if (!opened) {
      return
    }

    activeTab.value = 'basic'
  },
)

watch(
  () => props.customer,
  value => {
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
  value => {
    siteRows.resetRows(value)
  },
  {
    immediate: true,
  },
)

watch(
  () => props.employees,
  value => {
    employeeRows.resetRows(value)
  },
  {
    immediate: true,
  },
)

const {
  tabs: formTabs,
  fields,
} = useCustomerFormFields()

const {
  columns: siteColumns,
} = useCustomerSiteColumns()

const {
  columns: employeeColumns,
} = useCustomerEmployeeColumns()

const siteFilterRules = computed(() =>
  createSimpleTableFilterRules<CustomerSite>(
    siteColumns.value,
  ),
)

const employeeFilterRules = computed(() =>
  createSimpleTableFilterRules<CustomerEmployee>(
    employeeColumns.value,
  ),
)

const schema = z.object({
  name: z
    .string()
    .trim()
    .min(1, '顧客名は必須です'),
}).passthrough()

function handleClose() {
  dialogModel.value = false
}

function handleSave() {
  emit('save', {
    customer: {
      ...form,
    },

    sites: [
      ...siteRows.rows.value,
    ],

    employees: [
      ...employeeRows.rows.value,
    ],
  })
}

function handleDelete() {
  if (props.isCreateMode) {
    return
  }

  const confirmed = window.confirm(
    `顧客「${form.name}」を削除しますか？`,
  )

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
</script>

<template>
  <v-dialog
    v-model="dialogModel"
    max-width="1600"
    scrollable
  >
    <v-card class="customer-edit-dialog">
      <v-card-title>
        {{
          isCreateMode
            ? '顧客 新規登録'
            : '顧客 編集'
        }}
      </v-card-title>

      <v-divider />

      <v-card-text class="dialog-body">
        <TabLayout
          v-model="activeTab"
          :tabs="pageTabs"
        >
          <template #default="{ active }">
            <div v-if="active === 'basic'">
              <FormLayout
                v-model="form"
                :schema="schema"
              >
                <TabbedForm
                  v-model="form"
                  :tabs="[...formTabs]"
                  :fields="fields"
                />
              </FormLayout>
            </div>

            <div
              v-else-if="active === 'sites'"
            >
              <div class="child-toolbar">
                <v-btn
                  color="primary"
                  prepend-icon="mdi-plus"
                  @click="addSite"
                >
                  現場追加
                </v-btn>

                <v-btn
                  color="error"
                  variant="tonal"
                  prepend-icon="mdi-delete-outline"
                  :disabled="
                    !siteRows.hasDeleteSelected.value
                  "
                  @click="
                    siteRows.markSelectedDeleted
                  "
                >
                  選択行を削除
                </v-btn>
              </div>

              <div class="table-wrapper">
                <SimpleTable
                  table-key="customer-sites"
                  item-key="id"
                  :items="
                    siteRows.visibleRows.value
                  "
                  :columns="siteColumns"
                  :filter-rules="
                    siteFilterRules
                  "
                  @update:items="
                    siteRows.updateCell
                  "
                />
              </div>
            </div>

            <div
              v-else-if="active === 'employees'"
            >
              <div class="child-toolbar">
                <v-btn
                  color="primary"
                  prepend-icon="mdi-plus"
                  @click="addEmployee"
                >
                  顧客社員追加
                </v-btn>

                <v-btn
                  color="error"
                  variant="tonal"
                  prepend-icon="mdi-delete-outline"
                  :disabled="
                    !employeeRows.hasDeleteSelected.value
                  "
                  @click="
                    employeeRows.markSelectedDeleted
                  "
                >
                  選択行を削除
                </v-btn>
              </div>

              <div class="table-wrapper">
                <SimpleTable
                  table-key="customer-employees"
                  item-key="id"
                  :items="
                    employeeRows.visibleRows.value
                  "
                  :columns="employeeColumns"
                  :filter-rules="
                    employeeFilterRules
                  "
                  @update:items="
                    employeeRows.updateCell
                  "
                />
              </div>
            </div>

            <CustomerBillingRateTab
              v-else-if="
                active === 'billingRates'
              "
              :customer-id="customerId"
              :sites="siteRows.rows.value"
              :is-create-mode="isCreateMode"
              @saved="handleBillingRateSaved"
            />
          </template>
        </TabLayout>
      </v-card-text>

      <v-divider />

      <v-card-actions>
        <v-btn
          v-if="!isCreateMode"
          color="error"
          variant="text"
          @click="handleDelete"
        >
          顧客削除
        </v-btn>

        <v-spacer />

        <v-btn
          variant="text"
          @click="handleClose"
        >
          キャンセル
        </v-btn>

        <v-btn
          color="primary"
          @click="handleSave"
        >
          顧客情報を保存
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.customer-edit-dialog {
  height: 92vh;
  display: flex;
  flex-direction: column;
}

.dialog-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.child-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.table-wrapper {
  max-width: 100%;
  overflow: auto;
}
</style>