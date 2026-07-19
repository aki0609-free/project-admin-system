<script setup lang="ts">
import { computed, toRef } from 'vue'
import DetailDialogLayout from '@/toolbox/dialog/DetailDialogLayout.vue'
import type {
  EmployeeResignRequest,
  EmployeeResignationChecklistResponse,
} from '../types/employeeApiTypes'
import type { EmployeeForm } from '../types/employeeFormTypes'
import { useEmployeeResignDialog } from '../composables/useEmployeeResignDialog'

const props = defineProps<{
  modelValue: boolean
  employee: EmployeeForm
  checklist: EmployeeResignationChecklistResponse[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', request: EmployeeResignRequest): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const requiredChecklistIds = computed(() =>
  props.checklist
    .filter((item) => item.requiredFlag)
    .map((item) => item.id),
)

const {
  formModel,
  missingRequiredIds,
  canSubmit,
  footerItems,
} = useEmployeeResignDialog(
  visible,
  toRef(props, 'employee'),
  requiredChecklistIds,
  (request) => emit('submit', request),
)

const isChecked = (id: number): boolean =>
  formModel.checkedChecklistIds.includes(id)

const toggleChecklist = (id: number, checked: boolean) => {
  if (checked) {
    if (!formModel.checkedChecklistIds.includes(id)) {
      formModel.checkedChecklistIds.push(id)
    }
    return
  }

  formModel.checkedChecklistIds = formModel.checkedChecklistIds.filter(
    (checkedId) => checkedId !== id,
  )
}

const isMissing = (id: number): boolean =>
  missingRequiredIds.value.includes(id)
</script>

<template>
  <DetailDialogLayout
    v-model="visible"
    title="退職処理"
    max-width="760"
    :footer-items="footerItems"
  >
    <div class="resign-dialog">
      <div class="warning-card">
        <div class="warning-title">退職処理を実行します</div>
        <div class="warning-text">
          実行すると、従業員の在籍状態は退職、activeFlag は false になります。
          退職日と確認項目を確認してから実行してください。
        </div>
      </div>

      <div class="employee-card">
        <div class="employee-name">
          {{ employee.employeeCode }} / {{ employee.employeeName }}
        </div>
        <div class="employee-status">
          現在の状態：{{ employee.employmentStatus }} /
          activeFlag: {{ employee.activeFlag ? 'true' : 'false' }}
        </div>
      </div>

      <v-text-field
        v-model="formModel.resignDate"
        type="date"
        label="退職日"
        variant="outlined"
        density="comfortable"
        hide-details="auto"
      />

      <div class="checklist-section">
        <div class="section-title">退職前確認チェックリスト</div>

        <div
          v-for="item in checklist"
          :key="item.id"
          class="checklist-item"
          :class="{ missing: isMissing(item.id) }"
        >
          <v-checkbox
            :model-value="isChecked(item.id)"
            :label="item.name"
            density="compact"
            hide-details
            @update:model-value="toggleChecklist(item.id, Boolean($event))"
          />

          <v-chip
            v-if="item.requiredFlag"
            size="x-small"
            color="error"
            variant="tonal"
          >
            必須
          </v-chip>

          <div v-if="item.description" class="checklist-description">
            {{ item.description }}
          </div>
        </div>
      </div>

      <v-textarea
        v-model="formModel.note"
        label="備考"
        variant="outlined"
        rows="3"
        auto-grow
        hide-details
      />

      <div v-if="!canSubmit" class="cannot-submit">
        退職日を入力し、必須チェック項目をすべて確認してください。
      </div>
    </div>
  </DetailDialogLayout>
</template>

<style scoped>
.resign-dialog {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.warning-card {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #fed7aa;
  background: #fff7ed;
}

.warning-title {
  font-size: 15px;
  font-weight: 700;
  color: #9a3412;
}

.warning-text {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #7c2d12;
}

.employee-card {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.employee-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.employee-status {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.checklist-section {
  display: grid;
  gap: 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.checklist-item {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.checklist-item.missing {
  border-color: #fca5a5;
  background: #fff1f2;
}

.checklist-description {
  grid-column: 1 / -1;
  margin-left: 36px;
  margin-top: -4px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.cannot-submit {
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff1f2;
  color: #be123c;
  font-size: 13px;
  font-weight: 600;
}
</style>