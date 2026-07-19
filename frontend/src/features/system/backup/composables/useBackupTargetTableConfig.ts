import { computed, type Ref } from 'vue'
import type {
  SimpleTableColumnDef,
  SimpleTableEditableRow,
} from '@/shared/components/table/simple_table/types/item/types'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import type { BackupTargetResponse } from '@/features/system/backup/types/backupApiTypes'
import { getLastSegment, truncateText } from '@/shared/utils/StringUtils'

export type BackupTargetTableRow = SimpleTableEditableRow & {
  id: number
  selected: boolean
  targetCode: string
  targetName: string
  tableName: string
  outputMode: 'DOWNLOAD' | 'SERVER_FILE' | 'BOTH'
  outputDir: string
  fileNamePattern: string
  includeHeaderText: string
  zipRequiredText: string
  backupEnabledText: string
  activeText: string
  description: string
  raw: BackupTargetResponse
}

export const useBackupTargetTableConfig = (
  items: Ref<BackupTargetResponse[]>,
  selectedCodes: Ref<string[]>,
) => {
  const rows = computed<BackupTargetTableRow[]>(() =>
    items.value.map((item) => ({
      id: item.id,
      selected: selectedCodes.value.includes(item.targetCode),
      targetCode: item.targetCode,
      targetName: item.targetName,
      tableName: item.tableName,
      outputMode: item.outputMode,
      outputDir: item.outputDir ?? '',
      fileNamePattern: item.fileNamePattern ?? '',
      includeHeaderText: item.includeHeader ? 'あり' : 'なし',
      zipRequiredText: item.zipRequired ? 'あり' : 'なし',
      backupEnabledText: item.backupEnabled ? '対象' : '対象外',
      activeText: item.activeFlag ? '有効' : '無効',
      description: item.description ?? '',
      raw: item,
    })),
  )

  const columns = computed(() => {
    const defs: SimpleTableColumnDef<BackupTargetTableRow>[] = [
      { title: '選択', key: 'selected', type: 'checkbox', width: '83px', editable: true, filter: { type: 'checkbox' } },
      { title: 'targetCode', key: 'targetCode', type: 'text', width: '260px', filter: { type: 'text' } },
      { title: 'targetName', key: 'targetName', type: 'text', width: '180px', filter: { type: 'text' } },
      { title: 'tableName', key: 'tableName', type: 'text', width: '180px', filter: { type: 'text' } },
      { title: 'outputMode', key: 'outputMode', type: 'text', width: '180px', filter: { type: 'text' } },
      { title: 'outputDir', key: 'outputDir', type: 'text', width: '180px', filter: { type: 'text' }, formatter: (value) => truncateText(value as string, 10, '.../' + getLastSegment(value as string)) },
      { title: 'fileNamePattern', key: 'fileNamePattern', type: 'text', width: '260px', filter: { type: 'text' } },
      { title: 'Header', key: 'includeHeaderText', type: 'text', width: '120px', filter: { type: 'text' } },
      { title: 'ZIP', key: 'zipRequiredText', type: 'text', width: '120px', filter: { type: 'text' } },
      { title: 'Backup', key: 'backupEnabledText', type: 'text', width: '120px', filter: { type: 'text' } },
      { title: '状態', key: 'activeText', type: 'text', width: '120px', filter: { type: 'text' } },
      { title: '説明', key: 'description', type: 'text', width: '260px', filter: { type: 'text' } },
    ]

    return defs
  })

  const filterRules = computed(() =>
    createSimpleTableFilterRules<BackupTargetTableRow>(
      columns.value.filter((col) => col.key !== 'selected'),
    ),
  )

  return {
    rows,
    columns,
    filterRules,
  }
}