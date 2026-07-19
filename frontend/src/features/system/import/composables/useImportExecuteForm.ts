import { computed, ref, type Ref } from 'vue'
import type {
  ImportSourceType,
  ImportTargetResponse,
} from '@/features/system/import/types/importApiTypes'

export type ImportExecuteFormPayload = {
  targetCode: string
  sourceType: ImportSourceType
  file?: File | null
}

export const useImportExecuteForm = (
  targets: Ref<ImportTargetResponse[]>,
  pending: Ref<boolean>,
  emitExecute: (payload: ImportExecuteFormPayload) => void,
) => {
  const selectedTargetCode = ref('')
  const selectedFile = ref<File | null>(null)

  const selectedTarget = computed(() =>
    targets.value.find((item) => item.targetCode === selectedTargetCode.value) ?? null,
  )

  const needsFile = computed(() =>
    selectedTarget.value?.sourceType === 'UPLOAD',
  )

  const canExecute = computed(() =>
    selectedTargetCode.value !== ''
    && (!needsFile.value || selectedFile.value != null)
    && !pending.value,
  )

  const targetOptions = computed(() =>
    targets.value.map((target) => ({
      title: `${target.targetName} / ${target.tableName} / ${target.sourceType}`,
      value: target.targetCode,
    })),
  )

  const onFileChange = (files: File | File[] | null) => {
    if (Array.isArray(files)) {
      selectedFile.value = files[0] ?? null
      return
    }

    selectedFile.value = files ?? null
  }

  const execute = () => {
    if (!selectedTarget.value) return
    if (needsFile.value && !selectedFile.value) return

    emitExecute({
      targetCode: selectedTarget.value.targetCode,
      sourceType: selectedTarget.value.sourceType,
      file: selectedFile.value,
    })
  }

  return {
    selectedTargetCode,
    selectedFile,
    selectedTarget,
    needsFile,
    canExecute,
    targetOptions,
    onFileChange,
    execute,
  }
}