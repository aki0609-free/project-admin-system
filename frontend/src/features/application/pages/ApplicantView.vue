<script setup lang="ts">
import { ref } from 'vue'
import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'

import ApplicantTableTab from '@/features/application/components/applicant/ApplicantTableTab.vue'
import ApplicantChartTab from '@/features/application/components/applicant/ApplicantChartTab.vue'
import ApplicantAnalysisTab from '@/features/application/components/applicant/ApplicantAnalysisTab.vue'

import { useApplicantSource } from '@/features/application/composables/applicant/useApplicantSource'
import type { ApplicantPersistedRow } from '@/features/application/types/applicantTypes'

const tabs = [
  { label: 'テーブル', value: 'table' },
  { label: 'チャート', value: 'chart' },
  { label: '分析', value: 'analysis' },
]

const activeTab = ref('table')

const {
  applicants,
  createApplicant,
  updateApplicant,
  deleteApplicant,
  isSaving,
} = useApplicantSource()

const handleCreateApplicant = async (payload: ApplicantPersistedRow) => {
  await createApplicant(payload)
}

const handleUpdateApplicant = async (payload: ApplicantPersistedRow) => {
  await updateApplicant(payload)
}

const handleDeleteApplicant = async (id: number) => {
  await deleteApplicant(id)
}
</script>

<template>
  <ListDetailPageLayout
    title="応募者管理"
    description="応募者情報、応募経路、採用状況を管理します。"
  >
    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <ApplicantTableTab
          v-if="active === 'table'"
          :applicants="applicants"
          :saving="isSaving"
          @create="handleCreateApplicant"
          @update="handleUpdateApplicant"
          @delete="handleDeleteApplicant"
        />

        <ApplicantChartTab
          v-else-if="active === 'chart'"
          :applicants="applicants"
        />

        <ApplicantAnalysisTab
          v-else-if="active === 'analysis'"
          :applicants="applicants"
        />
      </template>
    </TabLayout>
  </ListDetailPageLayout>
</template>
