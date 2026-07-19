<script setup lang="ts">
import {
  computed,
  ref,
  watch,
} from 'vue'

import { z } from 'zod'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'

import { useEditableChildRows } from '../composables/useEditableChildRows'

import { useCustomerSiteBillingRatesQuery } from '../api/useCustomerSiteBillingRatesQuery'
import { useCreateCustomerSiteBillingRateMutation } from '../api/useCreateCustomerSiteBillingRateMutation'
import { useUpdateCustomerSiteBillingRateMutation } from '../api/useUpdateCustomerSiteBillingRateMutation'
import { useDeleteCustomerSiteBillingRateMutation } from '../api/useDeleteCustomerSiteBillingRateMutation'

import {
  createEmptyCustomerSiteBillingRate,
  toCustomerSiteBillingRate,
  toCustomerSiteBillingRateRequest,
} from '../mapper/customerSiteBillingRateMapper'

import type {
  CustomerSite,
  CustomerSiteBillingRate,
} from '../types/customerTypes'
import { useCustomerBillingRateColumns } from '../composables/useCustomerSiteBillingRateColumns'

const props = defineProps<{
  customerId: number | null
  sites: CustomerSite[]
  isCreateMode: boolean
}>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

const editableRows =
  useEditableChildRows<CustomerSiteBillingRate>()

const {
  billingRates,
  isFetching,
  refetch,
} = useCustomerSiteBillingRatesQuery(
  computed(() => props.customerId),
)

const createMutation =
  useCreateCustomerSiteBillingRateMutation()

const updateMutation =
  useUpdateCustomerSiteBillingRateMutation()

const deleteMutation =
  useDeleteCustomerSiteBillingRateMutation()

const {
  columns,
} = useCustomerBillingRateColumns(
  computed(() => props.sites),
)

const filterRules = computed(() =>
  createSimpleTableFilterRules<CustomerSiteBillingRate>(
    columns.value,
  ),
)

const saving = ref(false)

const persistedSites = computed(() =>
  props.sites.filter(site =>
    !site._isDeleted
    && !site._isNew
    && site.id > 0,
  ),
)

const canEdit = computed(() =>
  !props.isCreateMode
  && props.customerId != null
  && props.customerId > 0,
)

const loading = computed(() =>
  isFetching.value
  || saving.value
  || createMutation.isPending.value
  || updateMutation.isPending.value
  || deleteMutation.isPending.value,
)

watch(
  billingRates,
  value => {
    editableRows.resetRows(
      value.map(toCustomerSiteBillingRate),
    )
  },
  {
    immediate: true,
  },
)

watch(
  () => props.customerId,
  async customerId => {
    if (
      customerId == null
      || customerId <= 0
    ) {
      editableRows.resetRows([])
      return
    }

    await refetch()
  },
)

const rowSchema = z.object({
  customerSiteId: z
    .number()
    .positive('現場は必須です'),

  jobCode: z
    .string()
    .trim()
    .min(1, '職種コードは必須です'),

  jobName: z
    .string()
    .trim()
    .min(1, '職種名は必須です'),

  siteRoleCode: z
    .string()
    .trim()
    .min(1, '役職コードは必須です'),

  siteRoleName: z
    .string()
    .trim()
    .min(1, '現場役職は必須です'),

  billingUnit: z.enum([
    'HOURLY',
    'DAILY',
    'MONTHLY',
    'FIXED',
  ]),

  baseUnitPrice: z.coerce
    .number()
    .min(0, '基準単価は0以上で入力してください'),

  overtimeUnitPrice: z.coerce
    .number()
    .min(0, '残業単価は0以上で入力してください'),

  nightUnitPrice: z.coerce
    .number()
    .min(0, '深夜単価は0以上で入力してください'),

  commuteUnitPrice: z.coerce
    .number()
    .min(0, '通勤単価は0以上で入力してください'),

  effectiveFrom: z
    .string()
    .trim()
    .min(1, '適用開始日は必須です'),
})

function addBillingRate() {
  if (!canEdit.value) {
    return
  }

  if (persistedSites.value.length === 0) {
    window.alert(
      '請求単価を登録する前に、現場を1件以上保存してください。',
    )
    return
  }

  const visibleRows =
    editableRows.visibleRows.value

  const nextOrder =
    visibleRows.length === 0
      ? 1
      : Math.max(
          ...visibleRows.map(
            row => row.displayOrder ?? 0,
          ),
        ) + 1

  editableRows.addRow(
    createEmptyCustomerSiteBillingRate(
      persistedSites.value[0]?.id ?? null,
      nextOrder,
    ),
  )
}

function validateRows(): boolean {
  const errors: string[] = []

  editableRows.rows.value
    .filter(row => !row._isDeleted)
    .forEach((row, index) => {
      const result =
        rowSchema.safeParse(row)

      if (!result.success) {
        const message = result.error.issues
          .map(issue => issue.message)
          .join('、')

        errors.push(
          `${index + 1}行目：${message}`,
        )
      }

      if (
        row.effectiveFrom
        && row.effectiveTo
        && row.effectiveTo < row.effectiveFrom
      ) {
        errors.push(
          `${index + 1}行目：適用終了日は適用開始日以降にしてください`,
        )
      }
    })

  if (errors.length > 0) {
    window.alert(errors.join('\n'))
    return false
  }

  return true
}

async function saveBillingRates() {
  const customerId = props.customerId

  if (
    customerId == null
    || customerId <= 0
  ) {
    return
  }

  if (!validateRows()) {
    return
  }

  saving.value = true

  try {
    const deletedRows =
      editableRows.rows.value.filter(row =>
        row._isDeleted
        && !row._isNew
        && row.id > 0,
      )

    for (const row of deletedRows) {
      await deleteMutation.mutateAsync({
        customerId,
        billingRateId: row.id,
      })
    }

    const newRows =
      editableRows.rows.value.filter(row =>
        row._isNew
        && !row._isDeleted,
      )

    for (const row of newRows) {
      await createMutation.mutateAsync({
        customerId,
        body:
          toCustomerSiteBillingRateRequest(row),
      })
    }

    const updatedRows =
      editableRows.rows.value.filter(row =>
        !row._isNew
        && row._isUpdated
        && !row._isDeleted
        && row.id > 0,
      )

    for (const row of updatedRows) {
      await updateMutation.mutateAsync({
        customerId,
        billingRateId: row.id,
        body:
          toCustomerSiteBillingRateRequest(row),
      })
    }

    await refetch()

    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="billing-rate-tab">
    <v-alert
      v-if="isCreateMode"
      type="info"
      variant="tonal"
    >
      請求単価は、顧客と現場を一度保存した後に登録できます。
    </v-alert>

    <v-alert
      v-else-if="persistedSites.length === 0"
      type="warning"
      variant="tonal"
    >
      請求単価を登録するには、保存済みの現場が必要です。
    </v-alert>

    <template v-else>
      <v-alert
        type="info"
        variant="tonal"
      >
        現場・職種・現場役職・適用期間ごとに請求単価を登録します。
        日報保存時には、勤務日に適用される単価が日報へ保存されます。
      </v-alert>

      <div class="billing-toolbar">
        <v-btn
          color="primary"
          prepend-icon="mdi-plus"
          :disabled="!canEdit || loading"
          @click="addBillingRate"
        >
          単価追加
        </v-btn>

        <v-btn
          color="error"
          variant="tonal"
          prepend-icon="mdi-delete-outline"
          :disabled="
            !editableRows.hasDeleteSelected.value
            || loading
          "
          @click="editableRows.markSelectedDeleted"
        >
          選択行を削除
        </v-btn>

        <v-btn
          color="secondary"
          variant="tonal"
          prepend-icon="mdi-refresh"
          :disabled="!canEdit || loading"
          @click="refetch"
        >
          再読込
        </v-btn>

        <v-spacer />

        <v-btn
          color="primary"
          prepend-icon="mdi-content-save"
          :loading="saving"
          :disabled="!canEdit || loading"
          @click="saveBillingRates"
        >
          請求単価を保存
        </v-btn>
      </div>

      <v-progress-linear
        v-if="loading"
        indeterminate
      />

      <div class="billing-table-wrapper">
        <SimpleTable
          table-key="customer-billing-rates"
          item-key="id"
          :items="editableRows.visibleRows.value"
          :columns="columns"
          :filter-rules="filterRules"
          @update:items="editableRows.updateCell"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.billing-rate-tab {
  display: grid;
  gap: 12px;
}

.billing-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.billing-table-wrapper {
  max-width: 100%;
  overflow: auto;
}
</style>