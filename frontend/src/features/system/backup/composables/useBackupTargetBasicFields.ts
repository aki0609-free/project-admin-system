import { computed } from 'vue'
import type { GridFormFieldDef } from '@/shared/components/form/grid_based_form/types/types'
import type { BackupTargetDialogForm } from '@/features/system/backup/types/backupFormTypes'

export const useBackupTargetBasicFields = (
  formModel: BackupTargetDialogForm,
) => {
  const basicFields = computed<GridFormFieldDef<BackupTargetDialogForm>[]>(() => [
    {
      key: 'id',
      label: 'ID',
      type: 'number',
      width: 100,
      readonly: true,
    },
    {
      key: 'targetCode',
      label: 'targetCode',
      type: 'text',
      gridColumn: '2 / span 3',
    },
    {
      key: 'targetName',
      label: 'targetName',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'tableName',
      label: 'tableName',
      type: 'text',
      gridColumn: '3 / span 2',
    },
    {
      key: 'outputMode',
      label: 'outputMode',
      type: 'select',
      options: [
        { title: 'DOWNLOAD', value: 'DOWNLOAD' },
        { title: 'SERVER_FILE', value: 'SERVER_FILE' },
        { title: 'BOTH', value: 'BOTH' },
      ],
    },
    {
      key: 'outputDir',
      label: 'outputDir',
      type: 'text',
      gridColumn: '2 / span 3',
      disabled: formModel.outputMode === 'DOWNLOAD',
    },
    {
      key: 'fileNamePattern',
      label: 'fileNamePattern',
      type: 'text',
      gridColumn: '1 / span 4',
    },

    // CSV設定
    {
      key: 'includeHeader',
      label: 'includeHeader',
      type: 'checkbox',
      width: 140,
    },
    {
      key: 'zipRequired',
      label: 'zipRequired',
      type: 'checkbox',
      width: 120,
    },

    // 状態
    {
      key: 'backupEnabled',
      label: 'backupEnabled',
      type: 'checkbox',
      width: 140,
    },
    {
      key: 'activeFlag',
      label: 'activeFlag',
      type: 'checkbox',
      width: 120,
    },
  ])

  return {
    basicFields,
  }
}