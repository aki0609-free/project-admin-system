import { computed, type Ref } from 'vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { BatchJobDefinitionForm } from '@/features/system/batch/types/batchFormTypes'

export const useBatchDefinitionFooterItems = (
  visible: Ref<boolean>,
  isEdit: Ref<boolean>,
  formModel: BatchJobDefinitionForm,
  emitSave: (form: BatchJobDefinitionForm) => void,
  emitDelete: (form: BatchJobDefinitionForm) => void,
  emitExecuteNow: (form: BatchJobDefinitionForm) => void,
  emitExecuteScheduled: (form: BatchJobDefinitionForm) => void,
) => {
  const close = () => {
    visible.value = false
  }

  const snapshot = (): BatchJobDefinitionForm => ({
    ...formModel,
  })

  const leftFooterItems = computed<ToolbarItem[]>(() => {
    if (!isEdit.value) return []

    return [
      {
        type: 'button',
        label: '削除',
        color: 'error',
        intent: 'danger',
        onClick: () => emitDelete(snapshot()),
      },
      {
        type: 'button',
        label: '即時実行',
        color: 'secondary',
        intent: 'secondary',
        disabled: !formModel.immediateExecutable,
        onClick: () => emitExecuteNow(snapshot()),
      },
      {
        type: 'button',
        label: 'スケジュール実行テスト',
        color: 'secondary',
        intent: 'secondary',
        disabled: !formModel.scheduleEnabled,
        onClick: () => emitExecuteScheduled(snapshot()),
      },
    ]
  })

  const rightFooterItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '閉じる',
      color: 'secondary',
      intent: 'utility',
      onClick: close,
    },
    {
      type: 'button',
      label: '保存',
      color: 'primary',
      intent: 'primary',
      onClick: () => emitSave(snapshot()),
    },
  ])

  return {
    leftFooterItems,
    rightFooterItems,
  }
}
