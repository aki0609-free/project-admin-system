<script setup lang="ts">
import { computed, ref } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import type {
  DeductionDetailTableRow,
  DeductionDetailViewType,
} from '@/features/master/deduction/types/deductionTypes'
import type { DeductionDetailResponse } from '@/features/master/deduction/types/deductionApiTypes'
import { useDeductionDetailConfig } from '@/features/master/deduction/composables/useDeductionDetailConfig'
import { toDeductionDetailRows } from '@/features/master/deduction/mapper/deductionMapper'
import ResidentTaxEditorDialog from '@/features/master/deduction/components/ResidentTaxEditorDialog.vue'
import { useAuth } from '@/shared/auth/composables/useAuth'
import { Role } from '@/shared/auth/types/types'
import DateFormField from '@/shared/components/form/base/components/form/DateFormField.vue'
import { residentTaxEditorApi } from '@/features/master/deduction/api/residentTaxEditorApi'
import {
  createResidentTaxCsvTemplate,
  resolveResidentTaxFiscalYear,
} from '@/features/master/deduction/utils/residentTaxCsvTemplate'
import { downloadBlob } from '@/shared/utils/BusinessUtils'

const props = defineProps<{
  deductionId: number
  detailViewType: DeductionDetailViewType
  detailResponse: DeductionDetailResponse | null
  targetDate: string
}>()

const emit = defineEmits<{
  (e: 'update:targetDate', value: string): void
}>()

const targetDateModel = computed({
  get: () => props.targetDate,
  set: (value: string) => emit('update:targetDate', value),
})

const detail = useDeductionDetailConfig(computed(() => props.detailViewType))
const { hasRole } = useAuth()
const residentTaxEditorOpen = ref(false)
const residentTaxTemplateDownloading = ref(false)
const residentTaxTemplateError = ref('')
const canEditResidentTax = computed(() =>
  props.detailViewType === 'RESIDENT_TAX' && hasRole(Role.SYS_ADMIN),
)

const targetDateDescription = computed(() =>
  props.detailViewType === 'RESIDENT_TAX'
    ? '選択日の属する住民税年度（6月〜翌年5月）の確定値を参照します。'
    : '選択日時点で有効な税率・標準報酬などの控除情報を参照します。',
)

const rows = computed<DeductionDetailTableRow[]>(() =>
  toDeductionDetailRows(props.detailResponse?.details, props.detailViewType),
)

async function downloadResidentTaxTemplate() {
  residentTaxTemplateDownloading.value = true
  residentTaxTemplateError.value = ''

  try {
    const fiscalYear = resolveResidentTaxFiscalYear(props.targetDate)
    const response = await residentTaxEditorApi.find(fiscalYear)
    const csv = createResidentTaxCsvTemplate(response, fiscalYear)
    downloadBlob(
      new Blob([csv], { type: 'text/csv;charset=utf-8' }),
      `住民税取込フォーマット_${fiscalYear}年度.csv`,
    )
  } catch (error) {
    console.error(error)
    residentTaxTemplateError.value = '住民税取込フォーマットの作成に失敗しました。'
  } finally {
    residentTaxTemplateDownloading.value = false
  }
}
</script>

<template>
  <div class="d-flex flex-column ga-3">
    <div class="d-flex flex-wrap align-center ga-3">
      <DateFormField
        v-model="targetDateModel"
        label="参照基準日"
        variant="outlined"
        density="compact"
        hide-details
        style="width: 280px"
      />
      <div class="text-body-2 text-medium-emphasis">
        {{ targetDateDescription }}
      </div>
    </div>

    <div v-if="canEditResidentTax" class="d-flex flex-wrap justify-end ga-2">
      <v-btn
        variant="outlined"
        prepend-icon="mdi-file-delimited-outline"
        :loading="residentTaxTemplateDownloading"
        @click="downloadResidentTaxTemplate"
      >
        住民税CSVフォーマット
      </v-btn>
      <v-btn color="primary" prepend-icon="mdi-table-edit" @click="residentTaxEditorOpen = true">
        年度別住民税を編集
      </v-btn>
    </div>
    <v-alert v-if="residentTaxTemplateError" type="error" variant="tonal">
      {{ residentTaxTemplateError }}
    </v-alert>
    <div v-if="canEditResidentTax" class="text-caption text-medium-emphasis text-right">
      社員情報を入力済みで出力します。12か月分の税額を入力し、外部データ取込から登録してください。
    </div>
    <v-alert type="info" variant="tonal">
      <div class="font-weight-bold">{{ detail.title.value }}</div>
      <div>{{ detail.description.value }}</div>
      <div class="text-caption mt-1">
        計算根拠を確認するための読み取り専用表示です。
      </div>
    </v-alert>

    <v-alert v-if="rows.length === 0" type="warning" variant="tonal">
      選択した基準日に対応する詳細データが登録されていません。
    </v-alert>

    <SimpleTable
      v-else
      table-key="deduction-detail-reference"
      item-key="id"
      :items="rows"
      :columns="detail.columns.value"
      :filter-rules="detail.filterRules.value"
    />

    <ResidentTaxEditorDialog v-model="residentTaxEditorOpen" />
  </div>
</template>
