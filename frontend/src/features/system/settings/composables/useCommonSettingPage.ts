import { onMounted, ref } from 'vue'

import {
  createSystemSettingDefinition,
  deleteSystemSettingDefinition,
  getSystemSettingDefinitions,
  saveSystemSettings,
  updateSystemSettingDefinition,
} from '../api/commonSettingApi'
import type {
  SystemSettingDefinition,
  SystemSettingDefinitionCreateRequest,
  SystemSettingDefinitionUpdateRequest,
} from '../types/commonSettingTypes'

export const useCommonSettingPage = () => {
  const loading = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')
  const definitions = ref<SystemSettingDefinition[]>([])

  const run = async (action: () => Promise<void>) => {
    loading.value = true
    errorMessage.value = ''
    successMessage.value = ''
    try {
      await action()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '処理に失敗しました。'
    } finally {
      loading.value = false
    }
  }

  const load = () => run(async () => {
    definitions.value = await getSystemSettingDefinitions()
  })

  const createDefinition = (request: SystemSettingDefinitionCreateRequest) => run(async () => {
    await createSystemSettingDefinition(request)
    await loadData()
    successMessage.value = '会社独自の設定項目を追加しました。'
  })

  const updateDefinition = (
    id: number,
    request: SystemSettingDefinitionUpdateRequest,
  ) => run(async () => {
    await updateSystemSettingDefinition(id, request)
    await loadData()
    successMessage.value = '設定項目を更新しました。'
  })

  const deleteDefinition = (id: number) => run(async () => {
    await deleteSystemSettingDefinition(id)
    await loadData()
    successMessage.value = '設定項目を削除しました。'
  })

  const loadData = async () => {
    definitions.value = await getSystemSettingDefinitions()
  }

  const saveDefinitionValue = (definitionId: number, value: string) => run(async () => {
    await saveSystemSettings({
      values: [{
        definitionId,
        value,
      }],
    })
    await loadData()
    successMessage.value = '設定値を更新しました。'
  })

  onMounted(() => {
    void load()
  })

  return {
    loading,
    errorMessage,
    successMessage,
    definitions,
    saveDefinitionValue,
    createDefinition,
    updateDefinition,
    deleteDefinition,
  }
}
