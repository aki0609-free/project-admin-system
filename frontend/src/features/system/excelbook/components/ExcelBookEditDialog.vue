<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { z } from 'zod'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/toolbox/toolbar/types/types'
import type { ExcelBookMasterForm } from '../types/excelBookTypes'
import { createEmptyExcelBookForm } from '../utils/excelBookFactory'

const props = defineProps<{
  modelValue: boolean
  item: ExcelBookMasterForm | null
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save' | 'delete', value: ExcelBookMasterForm): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const form = reactive<ExcelBookMasterForm>(createEmptyExcelBookForm())

const schema = z.object({
  bookCode: z.string().min(1, 'Book Codeは必須です'),
  bookName: z.string().min(1, '名称は必須です'),
  templateFilePath: z.string().min(1, 'テンプレートファイルパスは必須です'),
  outputFilePath: z.string().min(1, '出力ファイルパスは必須です'),
  sourceType: z.literal('SNAPSHOT'),
  sourceName: z.string().min(1, 'Source Nameは必須です'),
  templateSheetName: z.string().min(1, 'テンプレートシート名は必須です'),
  activeFlag: z.boolean(),
})

const fields = computed<GridFormFieldDef<ExcelBookMasterForm>[]>(() => [
  {
    key: 'bookCode',
    label: 'Book Code',
    type: 'text',
    gridColumn: '1 / span 2',
    disabled: !form._isNew,
  },
  {
    key: 'bookName',
    label: '名称',
    type: 'text',
    gridColumn: '3 / span 2',
  },
  {
    key: 'sourceType',
    label: 'Source Type',
    type: 'select',
    options: [{ title: 'SNAPSHOT', value: 'SNAPSHOT' }],
    gridColumn: '1 / span 2',
  },
  {
    key: 'sourceName',
    label: 'Source Name',
    type: 'text',
    placeholder: 'monthly_site_summary_snapshot',
    gridColumn: '3 / span 2',
  },
  {
    key: 'templateFilePath',
    label: 'テンプレートファイルパス',
    type: 'text',
    gridColumn: '1 / span 4',
  },
  {
    key: 'outputFilePath',
    label: '出力ファイルパス',
    type: 'text',
    gridColumn: '1 / span 4',
  },
  {
    key: 'templateSheetName',
    label: 'テンプレートシート名',
    type: 'text',
    gridColumn: '1 / span 2',
  },
  {
    key: 'activeFlag',
    label: '有効',
    type: 'checkbox',
    gridColumn: '3 / span 1',
  },
])

const title = computed(() =>
  form._isNew ? 'Excel台帳マスタ 新規作成' : `Excel台帳マスタ：${form.bookName}`,
)

const leftFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '削除',
    color: 'error',
    disabled: form._isNew || props.saving,
    onClick: () => emit('delete', { ...form }),
  },
])

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: 'キャンセル',
    color: 'secondary',
    disabled: props.saving,
    onClick: () => {
      visible.value = false
    },
  },
  {
    type: 'button',
    label: '保存',
    color: 'primary',
    disabled: props.saving,
    onClick: () => emit('save', { ...form }),
  },
])

watch(
  () => props.item,
  value => {
    Object.assign(form, value ?? createEmptyExcelBookForm())
  },
  { immediate: true },
)
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    :title="title"
    max-width="1100"
    :left-footer-items="leftFooterItems"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout v-model="form" :schema="schema">
      <GridBasedForm
        v-model="form"
        :fields="fields"
      />
    </FormLayout>
  </DetailDialogLayout>
</template>