<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { ImportTargetResponse } from '@/features/system/import/types/importApiTypes'
import {
  type ImportExecuteFormPayload,
  useImportExecuteForm,
} from '@/features/system/import/composables/useImportExecuteForm'

const props = defineProps<{
  targets: ImportTargetResponse[]
  pending: boolean
}>()

const emit = defineEmits<{
  (e: 'execute', payload: ImportExecuteFormPayload): void
}>()

const pendingRef = computed(() => props.pending)

const {
  selectedTargetCode,
  selectedFile,
  selectedTarget,
  needsFile,
  canExecute,
  targetOptions,
  onFileChange,
  execute,
} = useImportExecuteForm(
  toRef(props, 'targets'),
  pendingRef,
  (payload) => emit('execute', payload),
)
</script>

<template>
  <v-card rounded="lg" variant="outlined">
    <v-card-text class="form-card">
      <v-select
        v-model="selectedTargetCode"
        label="取込定義"
        :items="targetOptions"
        variant="outlined"
        density="comfortable"
        hide-details
      />

      <v-card
        v-if="selectedTarget"
        variant="tonal"
        rounded="lg"
      >
        <v-card-text class="target-info">
          <div class="info-row">
            <span class="info-label">コード</span>
            <span class="info-value">{{ selectedTarget.targetCode }}</span>
          </div>

          <div class="info-row">
            <span class="info-label">名称</span>
            <span class="info-value">{{ selectedTarget.targetName }}</span>
          </div>

          <div class="info-row">
            <span class="info-label">テーブル</span>
            <span class="info-value">{{ selectedTarget.tableName }}</span>
          </div>

          <div class="info-row">
            <span class="info-label">取込元</span>
            <span class="info-value">{{ selectedTarget.sourceType }}</span>
          </div>

          <div class="info-row">
            <span class="info-label">取込方式</span>
            <span class="info-value">{{ selectedTarget.importMode }}</span>
          </div>

          <div
            v-if="selectedTarget.sourceType !== 'UPLOAD'"
            class="info-row"
          >
            <span class="info-label">CSVパス</span>
            <span class="info-value">{{ selectedTarget.fixedFilePath || '-' }}</span>
          </div>

          <div
            v-if="selectedTarget.sourceType === 'SCRIPT'"
            class="info-row"
          >
            <span class="info-label">Script</span>
            <span class="info-value">
              {{ selectedTarget.scriptType }} / {{ selectedTarget.scriptPath || '-' }}
            </span>
          </div>
        </v-card-text>
      </v-card>

      <v-file-input
        v-if="needsFile"
        v-model="selectedFile"
        label="CSVファイル"
        accept=".csv,text/csv"
        variant="outlined"
        density="comfortable"
        hide-details
        @update:model-value="onFileChange"
      />

      <v-card
        v-if="needsFile && selectedFile"
        variant="tonal"
        rounded="lg"
      >
        <v-card-text class="target-info">
          <div class="info-row">
            <span class="info-label">ファイル名</span>
            <span class="info-value">{{ selectedFile.name }}</span>
          </div>

          <div class="info-row">
            <span class="info-label">サイズ</span>
            <span class="info-value">{{ selectedFile.size.toLocaleString() }} bytes</span>
          </div>
        </v-card-text>
      </v-card>

      <div class="actions">
        <v-btn
          color="primary"
          variant="elevated"
          :loading="pending"
          :disabled="!canExecute"
          @click="execute"
        >
          インポート実行
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.form-card {
  display: grid;
  gap: 16px;
}

.target-info {
  display: grid;
  gap: 10px;
}

.info-row {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 12px;
}

.info-label {
  font-weight: 600;
  color: #475569;
}

.info-value {
  color: #0f172a;
  word-break: break-word;
}

.actions {
  display: flex;
  justify-content: flex-end;
}
</style>