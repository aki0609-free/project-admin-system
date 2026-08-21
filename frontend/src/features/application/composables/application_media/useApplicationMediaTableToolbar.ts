import { computed, type ComputedRef } from 'vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { FilterState } from '@/features/application/types/applicationMediaTypes'

type Params = {
  allYearMonthOptions: ComputedRef<{ label: string; value: string }[]>
  filter: FilterState
  openMediaDialog: () => void
  openYearMonthDialog: () => void
  openDeleteMediaDialog: () => void
  openDeleteYearMonthDialog: () => void
  isDirty: ComputedRef<boolean>
  saving: ComputedRef<boolean>
  onSave: () => void | Promise<void>
}

export const useApplicationMediaTableToolbar = ({
  allYearMonthOptions,
  filter,
  openMediaDialog,
  openYearMonthDialog,
  openDeleteMediaDialog,
  openDeleteYearMonthDialog,
  isDirty,
  saving,
  onSave,
}: Params) => {
  const leftToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'dropdown',
      label: '掲載年月',
      options: [
        { label: 'すべて', value: '' },
        ...allYearMonthOptions.value,
      ],
      onSelect: (value: string) => {
        filter.mediaYearMonth = value
      },
    },
    {
      type: 'search',
      modelValue: filter.mediaName,
      placeholder: '応募媒体名',
      'onUpdate:modelValue': (value: string) => {
        filter.mediaName = value
      },
    },
  ])

  const rightToolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '媒体追加',
      intent: 'primary',
      onClick: openMediaDialog,
    },
    {
      type: 'button',
      label: '年月追加',
      intent: 'secondary',
      onClick: openYearMonthDialog,
    },
    {
      type: 'button',
      label: 'メディア削除',
      intent: 'danger',
      onClick: openDeleteMediaDialog,
    },
    {
      type: 'button',
      label: '年月削除',
      intent: 'danger',
      onClick: openDeleteYearMonthDialog,
    },
    {
      type: 'button',
      label: saving.value ? '保存中...' : '保存',
      intent: 'primary',
      disabled: !isDirty.value || saving.value,
      onClick: () => {
        void onSave()
      },
    } as ToolbarItem,
  ])

  return {
    leftToolbarItems,
    rightToolbarItems,
  }
}
