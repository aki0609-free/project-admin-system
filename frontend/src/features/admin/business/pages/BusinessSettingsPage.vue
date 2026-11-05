<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { getPublishedBusinessGenericMasters } from '@/features/system/masterdata/api/genericMasterApi'
import GenericMasterDataPanel from '@/features/system/masterdata/components/GenericMasterDataPanel.vue'
import type { GenericMasterDefinition } from '@/features/system/masterdata/types/genericMasterTypes'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'

const activeTab = ref('')
const loading = ref(false)
const errorMessage = ref('')
const genericMasterDefinitions = ref<GenericMasterDefinition[]>([])

onMounted(async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    genericMasterDefinitions.value = await getPublishedBusinessGenericMasters()
    const firstDefinition = genericMasterDefinitions.value[0]
    if (firstDefinition) activeTab.value = `generic-${firstDefinition.id}`
  } catch (error) {
    errorMessage.value = error instanceof Error
      ? error.message
      : '汎用マスター定義の取得に失敗しました。'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <ListDetailPageLayout
    title="業務管理"
    description="汎用マスターから公開された業務固有設定を管理します。"
  >
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>

    <v-progress-linear v-if="loading" indeterminate color="primary" />

    <v-alert
      v-else-if="genericMasterDefinitions.length === 0 && !errorMessage"
      type="info"
      variant="tonal"
    >
      業務管理へ公開されている汎用マスターはありません。
    </v-alert>

    <v-card
      v-else-if="genericMasterDefinitions.length > 0"
      variant="outlined"
      class="business-settings-card"
    >
      <v-tabs
        v-model="activeTab"
        color="primary"
        class="settings-tabs"
      >
        <v-tab
          v-for="definition in genericMasterDefinitions"
          :key="definition.id"
          :value="`generic-${definition.id}`"
        >
          {{ definition.tabName }}
        </v-tab>
      </v-tabs>

      <v-divider />

      <v-window
        v-model="activeTab"
        class="settings-window"
      >
        <v-window-item
          v-for="definition in genericMasterDefinitions"
          :key="definition.id"
          :value="`generic-${definition.id}`"
        >
          <GenericMasterDataPanel :definition="definition" />
        </v-window-item>
      </v-window>
    </v-card>
  </ListDetailPageLayout>
</template>

<style scoped>
.business-settings-card,
.settings-tabs,
.settings-window {
  background: rgb(var(--v-theme-surface));
}
</style>
