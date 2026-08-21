import { computed, reactive, ref, watch, type Ref } from 'vue'
import { z } from 'zod'

import type {
  NoticeContentFormat,
  NoticeCreateRequest,
  NoticeResponse,
  NoticeType,
} from '@/features/dashboard/types/dashboardTypes'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

export type NoticeDialogForm = Omit<NoticeCreateRequest, 'content'> & {
  content: string
}

export const noticeDialogSchema = z.object({
  title: z.string().min(1, '必須です'),
  start: z.string().min(1, '必須です'),
  end: z.string().min(1, '必須です'),
  type: z.enum(['IMPORTANT', 'WARNING', 'INFO']),
  color: z.string().min(1, '必須です'),
  contentFormat: z.enum(['PLAIN_TEXT', 'HTML', 'MARKDOWN']),
  content: z.string(),
  pinnedFlag: z.boolean(),
  activeFlag: z.boolean(),
})

export const useNoticeEditDialog = (
  visible: Ref<boolean>,
  notice: Ref<NoticeResponse | null | undefined>,
  emitSubmit: (value: NoticeCreateRequest) => void,
) => {
  const activeTab = ref<'edit' | 'preview'>('edit')

  const form = reactive<NoticeDialogForm>({
    title: '',
    start: '',
    end: '',
    type: 'INFO',
    color: 'blue',
    contentFormat: 'PLAIN_TEXT',
    content: '',
    pinnedFlag: false,
    activeFlag: true,
  })

  const isEdit = computed(() => !!notice.value?.id)

  const typeItems: { title: string; value: NoticeType }[] = [
    { title: '重要', value: 'IMPORTANT' },
    { title: '警告', value: 'WARNING' },
    { title: '情報', value: 'INFO' },
  ]

  const colorItems = [
    { title: '青', value: 'blue' },
    { title: '赤', value: 'red' },
    { title: 'オレンジ', value: 'orange' },
    { title: '緑', value: 'green' },
    { title: '紫', value: 'purple' },
  ]

  const contentFormatItems: { title: string; value: NoticeContentFormat }[] = [
    { title: '通常テキスト', value: 'PLAIN_TEXT' },
    { title: 'リッチテキスト', value: 'HTML' },
    { title: 'Markdown', value: 'MARKDOWN' },
  ]

  const fields = computed<GridFormFieldDef<NoticeDialogForm>[]>(() => [
    {
      key: 'title',
      label: 'タイトル',
      type: 'text',
      gridColumn: '1 / -1',
      required: true,
    },
    { key: 'start', label: '開始日', type: 'date', required: true },
    { key: 'end', label: '終了日', type: 'date', required: true },
    { key: 'type', label: '種別', type: 'select', options: typeItems },
    { key: 'color', label: '色', type: 'select', options: colorItems },
    {
      key: 'contentFormat',
      label: '本文形式',
      type: 'select',
      options: contentFormatItems,
    },
    { key: 'pinnedFlag', label: '固定表示する', type: 'checkbox' },
    { key: 'activeFlag', label: '有効', type: 'checkbox' },
  ])

  const resetForCreate = () => {
    const today = new Date().toISOString().slice(0, 10)

    form.title = ''
    form.start = today
    form.end = today
    form.type = 'INFO'
    form.color = 'blue'
    form.contentFormat = 'PLAIN_TEXT'
    form.content = ''
    form.pinnedFlag = false
    form.activeFlag = true
    activeTab.value = 'edit'
  }

  const applyNotice = (value: NoticeResponse) => {
    form.title = value.title ?? ''
    form.start = value.start ?? ''
    form.end = value.end ?? ''
    form.type = value.type ?? 'INFO'
    form.color = value.color ?? 'blue'
    form.contentFormat = value.contentFormat ?? 'PLAIN_TEXT'
    form.content = value.content ?? ''
    form.pinnedFlag = value.pinnedFlag ?? false
    form.activeFlag = value.activeFlag ?? true
    activeTab.value = 'edit'
  }

  const syncForm = () => {
    if (notice.value) {
      applyNotice(notice.value)
      return
    }

    resetForCreate()
  }

  watch(
    () => visible.value,
    opened => {
      if (!opened) return
      syncForm()
    },
  )

  watch(
    () => notice.value,
    () => {
      if (!visible.value) return
      syncForm()
    },
  )

  const submit = () => {
    emitSubmit({
      ...form,
      content: form.content || null,
    })
  }

  const close = () => {
    visible.value = false
  }

  const rightFooterItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: 'キャンセル',
      intent: 'secondary',
      onClick: close,
    },
    {
      type: 'button',
      label: isEdit.value ? '更新' : '作成',
      intent: 'primary',
      onClick: submit,
    },
  ])

  return {
    form,
    activeTab,
    isEdit,
    typeItems,
    colorItems,
    contentFormatItems,
    fields,
    schema: noticeDialogSchema,
    rightFooterItems,
    submit,
  }
}
