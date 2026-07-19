<script setup lang="ts">
import { toRef } from 'vue'
import { formatCurrency } from '@/shared/utils/CurrencyUtils'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { useApplicationMediaAnalysis } from '@/features/application/composables/application_media/useApplicationMediaAnalysis'

const props = defineProps<{
  medias: ApplicationMediaLocalItem[]
}>()

const analysis = useApplicationMediaAnalysis(toRef(props, 'medias'))

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
          <v-card-title>総コスト</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ formatCurrency(analysis.totalCost.value) }}
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>総採用数</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ analysis.totalHires.value }}件
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>平均単価</v-card-title>
          <v-card-text class="text-h5 font-weight-bold">
            {{ formatCurrency(analysis.averageUnitPrice.value) }}
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>最も効率の良い媒体</v-card-title>
          <v-card-text v-if="analysis.bestEfficiencyMedia.value">
            <div class="text-h6">{{ analysis.bestEfficiencyMedia.value.mediaName }}</div>
            <div>単価: {{ formatCurrency(analysis.bestEfficiencyMedia.value.unitPrice) }}</div>
            <div>採用数: {{ analysis.bestEfficiencyMedia.value.hires }}件</div>
            <div>コスト: {{ formatCurrency(analysis.bestEfficiencyMedia.value.cost) }}</div>
          </v-card-text>
          <v-card-text v-else>データなし</v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>最もコストの高い媒体</v-card-title>
          <v-card-text v-if="analysis.highestCostMedia.value">
            <div class="text-h6">{{ analysis.highestCostMedia.value.mediaName }}</div>
            <div>コスト: {{ formatCurrency(analysis.highestCostMedia.value.cost) }}</div>
            <div>採用数: {{ analysis.highestCostMedia.value.hires }}件</div>
            <div>単価: {{ formatCurrency(analysis.highestCostMedia.value.unitPrice) }}</div>
          </v-card-text>
          <v-card-text v-else>データなし</v-card-text>
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