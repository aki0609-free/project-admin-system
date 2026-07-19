<script setup lang="ts">
import { ref } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'

import ApplicantTableBasicTab from '@/features/application/components/applicant/ApplicantTableBasicTab.vue'
import ApplicantTableRecruitmentTab from '@/features/application/components/applicant/ApplicantTableRecruitmentTab.vue'
import ApplicantTableProfileTab from '@/features/application/components/applicant/ApplicantTableProfileTab.vue'
import ApplicantEmploymentTableTab from '@/features/application/components/applicant/ApplicantEmploymentTableTab.vue'
import ApplicantTableMediaTab from '@/features/application/components/applicant/ApplicantTableMediaTab.vue'
import ApplicantFormDialog from '@/features/application/components/applicant/ApplicantFormDialog.vue'

import type {
  ApplicantPersistedRow,
  ApplicantRow,
} from '@/features/application/types/applicantTypes'
import { createEmptyApplicant } from '@/features/application/utils/createEmptyApplicantForm'
import { toApplicantPersistedRow } from '@/features/application/utils/applicantMediaFormMapper'

const props = defineProps<{
  applicants: ApplicantRow[]
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'create', payload: ApplicantPersistedRow): void
  (e: 'update', payload: ApplicantPersistedRow): void
  (e: 'delete', id: number): void
}>()

const tabs = [
  { label: '基本情報', value: 'basic' },
  { label: '応募情報', value: 'recruitment' },
  { label: '媒体情報', value: 'media' },
  { label: '属性情報', value: 'profile' },
  { label: '入退社情報', value: 'employment' },
]

const activeTab = ref('basic')
const dialog = ref(false)
const editingApplicant = ref<ApplicantPersistedRow | null>(null)
const isCreateMode = ref(false)

function handleCreate() {
  isCreateMode.value = true
  editingApplicant.value = createEmptyApplicant(props.applicants.length)
  dialog.value = true
}

function handleEdit(row: ApplicantRow) {
  isCreateMode.value = false
  editingApplicant.value = toApplicantPersistedRow(row)
  dialog.value = true
}

function handleSave(payload: ApplicantPersistedRow) {
  if (isCreateMode.value) {
    emit('create', payload)
  } else {
    emit('update', payload)
  }

  dialog.value = false
  editingApplicant.value = null
}

function handleDelete(id: number) {
  emit('delete', id)
  dialog.value = false
  editingApplicant.value = null
}
</script>

<template>
  <div class="d-flex flex-column ga-4">
    <div class="d-flex justify-start">
      <v-btn color="primary" :loading="props.saving" @click="handleCreate">
        新規登録
      </v-btn>
    </div>

    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <ApplicantTableBasicTab
          v-if="active === 'basic'"
          :applicants="props.applicants"
          @row-click="handleEdit"
        />

        <ApplicantTableRecruitmentTab
          v-else-if="active === 'recruitment'"
          :applicants="props.applicants"
          @row-click="handleEdit"
        />

        <ApplicantTableMediaTab
          v-else-if="active === 'media'"
          :applicants="props.applicants"
          @row-click="handleEdit"
        />

        <ApplicantTableProfileTab
          v-else-if="active === 'profile'"
          :applicants="props.applicants"
          @row-click="handleEdit"
        />

        <ApplicantEmploymentTableTab
          v-else-if="active === 'employment'"
          :applicants="props.applicants"
          @row-click="handleEdit"
        />
      </template>
    </TabLayout>

    <ApplicantFormDialog
      v-model="dialog"
      :applicant="editingApplicant"
      :is-create-mode="isCreateMode"
      @save="handleSave"
      @delete="handleDelete"
    />
  </div>
</template>