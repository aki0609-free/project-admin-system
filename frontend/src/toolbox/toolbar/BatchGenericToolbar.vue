<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import { useExecuteBatchMutation } from '@/features/system/batch/api/mutations/useExecuteBatchMutation'
import { useDownloadBatchLogFileMutation } from '@/features/system/batch/api/mutations/useDownloadBatchLogFileMutation'
import { downloadBlob } from '@/features/system/backup/utils/downloadBlob'
import type { ToolbarItem } from './types/types'
import type { BatchExecuteResponse } from '@/features/system/batch/types/batchApiTypes'
import BatchParameterDialog from '../dialog/BatchParameterDialog.vue'

defineProps({
  leftItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
  rightItems: {
    type: Array as PropType<ToolbarItem[]>,
    default: () => [],
  },
})

const executeBatchMutation = useExecuteBatchMutation()
const downloadBatchLogFileMutation = useDownloadBatchLogFileMutation()

const batchDialogVisible = ref(false)
const selectedBatchItem = ref<Extract<ToolbarItem, { type: 'batch' }> | null>(null)

const batchLoading = computed(
  () => executeBatchMutation.isPending.value || downloadBatchLogFileMutation.isPending.value,
)

const openBatchDialog = (item: Extract<ToolbarItem, { type: 'batch' }>) => {
  selectedBatchItem.value = item
  batchDialogVisible.value = true
}

const executeBatch = async (payload: {
  jobCode: string
  params: Record<string, unknown>
}) => {
  const item = selectedBatchItem.value
  const outputAction = item?.outputAction ?? 'none'
  const previewWindow = outputAction === 'preview'
    ? window.open('about:blank', '_blank')
    : null

  if (previewWindow) {
    previewWindow.opener = null
    previewWindow.document.title = '帳票を生成しています'
    previewWindow.document.body.textContent = '帳票を生成しています。しばらくお待ちください。'
  }

  try {
    const result = await executeBatchMutation.mutateAsync(payload) as BatchExecuteResponse

    batchDialogVisible.value = false

    if (result.status !== 'COMPLETED') {
      previewWindow?.close()
      alert(result.message)
      return
    }

    if (outputAction !== 'none' && result.logId) {
      const blob = await downloadBatchLogFileMutation.mutateAsync(result.logId) as Blob
      const fileName = result.outputFileName ?? `batch-output-${result.logId}`

      if (outputAction === 'preview') {
        const url = window.URL.createObjectURL(blob)
        if (previewWindow) {
          previewWindow.location.replace(url)
        } else {
          window.open(url, '_blank', 'noopener,noreferrer')
        }
        window.setTimeout(() => window.URL.revokeObjectURL(url), 5 * 60 * 1000)
      } else {
        downloadBlob(blob, fileName)
      }
    } else {
      previewWindow?.close()
    }

    if (item?.onSuccess) {
      item.onSuccess(result.message)
    } else {
      alert(result.message)
    }
  } catch (error) {
    previewWindow?.close()
    throw error
  }
}
</script>

<template>
  <v-toolbar
    flat
    density="comfortable"
    class="custom-toolbar px-4"
  >
    <div class="toolbar-inner">
      <div class="toolbar-side">
        <template
          v-for="(item, i) in leftItems"
          :key="`left-${i}`"
        >
          <v-btn
            v-if="item.type === 'button'"
            :color="item.color ?? 'primary'"
            variant="tonal"
            rounded="lg"
            :disabled="item.disabled ?? false"
            @click="item.onClick"
          >
            {{ item.label }}
          </v-btn>

          <v-btn
            v-else-if="item.type === 'batch'"
            :color="item.color ?? 'secondary'"
            variant="tonal"
            rounded="lg"
            :disabled="item.disabled ?? false"
            @click="openBatchDialog(item)"
          >
            {{ item.label }}
          </v-btn>

          <v-btn
            v-else-if="item.type === 'icon'"
            icon
            variant="text"
            class="icon-btn"
            :disabled="item.disabled ?? false"
            @click="item.onClick"
          >
            <v-icon>{{ item.icon }}</v-icon>
          </v-btn>

          <v-menu v-else-if="item.type === 'dropdown'">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                variant="tonal"
                rounded="lg"
                :disabled="item.disabled ?? false"
              >
                {{ item.label }}
              </v-btn>
            </template>

            <v-list density="compact">
              <v-list-item
                v-for="opt in item.options"
                :key="opt.value"
                @click="item.onSelect(opt.value)"
              >
                {{ opt.label }}
              </v-list-item>
            </v-list>
          </v-menu>

          <v-text-field
            v-else-if="item.type === 'search'"
            :placeholder="item.placeholder ?? 'Search...'"
            :model-value="item.modelValue"
            density="compact"
            hide-details
            variant="solo-filled"
            rounded="lg"
            class="search-field"
            @update:model-value="item['onUpdate:modelValue']"
          />
        </template>
      </div>

      <div class="toolbar-side">
        <template
          v-for="(item, i) in rightItems"
          :key="`right-${i}`"
        >
          <v-btn
            v-if="item.type === 'button'"
            :color="item.color ?? 'primary'"
            variant="tonal"
            rounded="lg"
            :disabled="item.disabled ?? false"
            @click="item.onClick"
          >
            {{ item.label }}
          </v-btn>

          <v-btn
            v-else-if="item.type === 'batch'"
            :color="item.color ?? 'secondary'"
            variant="tonal"
            rounded="lg"
            :disabled="item.disabled ?? false"
            @click="openBatchDialog(item)"
          >
            {{ item.label }}
          </v-btn>

          <v-btn
            v-else-if="item.type === 'icon'"
            icon
            variant="text"
            class="icon-btn"
            :disabled="item.disabled ?? false"
            @click="item.onClick"
          >
            <v-icon>{{ item.icon }}</v-icon>
          </v-btn>

          <v-menu v-else-if="item.type === 'dropdown'">
            <template #activator="{ props }">
              <v-btn
                v-bind="props"
                variant="tonal"
                rounded="lg"
                :disabled="item.disabled ?? false"
              >
                {{ item.label }}
              </v-btn>
            </template>

            <v-list density="compact">
              <v-list-item
                v-for="opt in item.options"
                :key="opt.value"
                @click="item.onSelect(opt.value)"
              >
                {{ opt.label }}
              </v-list-item>
            </v-list>
          </v-menu>

          <v-text-field
            v-else-if="item.type === 'search'"
            :placeholder="item.placeholder ?? 'Search...'"
            :model-value="item.modelValue"
            density="compact"
            hide-details
            variant="solo-filled"
            rounded="lg"
            class="search-field"
            @update:model-value="item['onUpdate:modelValue']"
          />
        </template>
      </div>
    </div>
  </v-toolbar>

  <BatchParameterDialog
    v-model="batchDialogVisible"
    :item="selectedBatchItem"
    :loading="batchLoading"
    @execute="executeBatch"
  />
</template>

<style scoped>
.custom-toolbar {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding-top: 4px;
  padding-bottom: 4px;
}

.toolbar-inner {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-side {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  min-width: 40px;
}

.search-field {
  max-width: 240px;
}
</style>
