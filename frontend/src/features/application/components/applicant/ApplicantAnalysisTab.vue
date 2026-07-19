<script setup lang="ts">
import { toRef } from 'vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { useApplicantAnalysis } from '@/features/application/composables/applicant/useApplicantAnalysis'

const props = defineProps<{
  applicants: ApplicantRow[]
}>()

const analysis = useApplicantAnalysis(toRef(props, 'applicants'))

const handleAiAnalysis = async () => {
  await analysis.analyzeByAi()
}
</script>

<template>
  <v-container fluid class="pa-0">
    <v-row>
      <v-col cols="12" class="d-flex justify-end">
        <v-btn
          color="primary"
          variant="elevated"
          :disabled="!analysis.canAnalyzeByAi.value"
          @click="handleAiAnalysis"
        >
          AI分析を実行
        </v-btn>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>総応募者数</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ analysis.totalApplicants.value }}名
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>面接実施数</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ analysis.interviewedCount.value }}名
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>在籍中人数</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ analysis.workingCount.value }}名
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>退職人数</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ analysis.resignedCount.value }}名
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>面接率</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ analysis.interviewRate.value }}%
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>在籍率</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ analysis.workingRate.value }}%
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>主要媒体</v-card-title>
          <v-card-text v-if="analysis.majorMedia.value">
            <div class="text-h6">{{ analysis.majorMedia.value.mediaName }}</div>
            <div>応募数: {{ analysis.majorMedia.value.count }}名</div>
          </v-card-text>
          <v-card-text v-else>
            データなし
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12">
        <v-alert
          v-if="!analysis.aiAnalysisEnabled"
          type="info"
          variant="tonal"
          class="mb-4"
        >
          AI分析はまだ未接続です。将来はこの画面から summary データを backend 経由で AI に渡します。
        </v-alert>

        <v-card>
          <v-card-title>分析コメント</v-card-title>
          <v-card-text>
            <v-list density="compact">
              <v-list-item
                v-for="comment in analysis.analysisComments.value"
                :key="comment"
              >
                {{ comment }}
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>