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
      label: '対象コード',
      type: 'text',
      gridColumn: '2 / span 3',
      readonly: formModel.id > 0,
    },
    {
      key: 'targetName',
      label: '対象名',
      type: 'text',
      gridColumn: '1 / span 2',
    },
    {
      key: 'tableName',
      label: 'テーブル名',
      type: 'text',
      gridColumn: '3 / span 2',
      readonly: formModel.id > 0,
    },
    {
      key: 'outputMode',
      label: '出力方法',
      type: 'select',
      options: [
        { title: 'ダウンロードのみ', value: 'DOWNLOAD' },
        { title: 'ストレージ保存のみ', value: 'SERVER_FILE' },
        { title: '保存＋ダウンロード', value: 'BOTH' },
      ],
    },
    {
      key: 'outputDir',
      label: '保存先サブフォルダ',
      type: 'text',
      gridColumn: '2 / span 3',
      disabled: formModel.outputMode === 'DOWNLOAD',
    },
    {
      key: 'fileNamePattern',
      label: 'ファイル名パターン',
      type: 'text',
      gridColumn: '1 / span 4',
    },

    // CSV設定
    {
      key: 'includeHeader',
      label: 'ヘッダーを含める',
      type: 'checkbox',
      width: 140,
    },
    {
      key: 'zipRequired',
      label: 'ZIP出力',
      type: 'checkbox',
      width: 120,
    },

    // 状態
    {
      key: 'backupEnabled',
      label: 'バックアップ対象',
      type: 'checkbox',
      width: 140,
    },
    {
      key: 'activeFlag',
      label: '有効',
      type: 'checkbox',
      width: 120,
    },
  ])

  return {
    basicFields,
  }
}
