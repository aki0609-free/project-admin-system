import { computed, reactive, ref, watch, type Ref } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'
import type { BackupTargetDialogForm } from '@/features/system/backup/types/backupFormTypes'
import {
  createEmptyBackupTargetForm,
  toBackupTargetDialogForm,
} from '@/features/system/backup/utils/backupFormFactory'
import { backupTargetSchema } from './useBackupTargetSchema'
import { useBackupTargetBasicFields } from './useBackupTargetBasicFields'

export const useBackupTargetEditDialog = (
  visible: Ref<boolean>,
  target: Ref<BackupTargetResponse | null>,
  emitSave: (form: BackupTargetDialogForm) => void,
  emitDelete: (form: BackupTargetDialogForm) => void,
) => {
  const activeTab = ref<'basic' | 'columns'>('basic')
  const formModel = reactive<BackupTargetDialogForm>(
    createEmptyBackupTargetForm(),
  )

  const resetForm = () => {
    Object.assign(formModel, createEmptyBackupTargetForm())
    activeTab.value = 'basic'
  }

  const applyTarget = (value: BackupTargetResponse) => {
    Object.assign(formModel, toBackupTargetDialogForm(value))
    activeTab.value = 'basic'
  }

  watch(
    () => visible.value,
    opened => {
      if (!opened) return

      if (!target.value) {
        resetForm()
        return
      }

      applyTarget(target.value)
    },
  )

  watch(
    () => target.value,
    value => {
      if (!visible.value) return

      if (!value) {
        resetForm()
        return
      }

      applyTarget(value)
    },
    { immediate: true },
  )

  const isEdit = computed(() => formModel.id > 0)

  const title = computed(() =>
    isEdit.value
      ? 'バックアップ対象編集'
      : 'バックアップ対象新規作成',
  )

  const tabs = [
    { label: '基本情報', value: 'basic' },
    { label: 'Column', value: 'columns' },
  ]

  const { basicFields } = useBackupTargetBasicFields(formModel)

  const snapshot = (): BackupTargetDialogForm => ({
    ...formModel,
    columns: formModel.columns
      .slice()
      .sort((a, b) => a.orderNo - b.orderNo)
      .map(item => ({ ...item })),
  })

  const close = () => {
    visible.value = false
  }

  const leftFooterItems = computed<ToolbarItem[]>(() => {
    if (!isEdit.value) return []

    return [
      {
        type: 'button',
        label: '削除',
        color: 'error',
        onClick: () => emitDelete(snapshot()),
      },
    ]
  })

  const rightFooterItems = computed<ToolbarItem[]>(() => [
    {
      type: 'button',
      label: '閉じる',
      color: 'secondary',
      onClick: close,
    },
    {
      type: 'button',
      label: '保存',
      color: 'primary',
      onClick: () => emitSave(snapshot()),
    },
  ])

  return {
    activeTab,
    formModel,
    isEdit,
    title,
    tabs,
    basicFields,
    schema: backupTargetSchema,
    leftFooterItems,
    rightFooterItems,
  }
}