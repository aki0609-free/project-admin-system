<script setup lang="ts">
import { toRef } from 'vue'
import GenericChart from '@/shared/components/chart/GenericChart.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { useApplicationMediaChart } from '@/features/application/composables/application_media/useApplicationMediaChart'

const props = defineProps<{
  medias: ApplicationMediaLocalItem[]
}>()

const chart = useApplicationMediaChart(toRef(props, 'medias'))
</script>

<template>
  <div class="d-flex flex-column ga-4">
    <GenericToolbar :items="chart.toolbarItems.value" />

    <v-container fluid class="pa-0">
      <v-row>
        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別コスト推移</v-card-title>
            <v-card-text>
              <GenericChart type="bar" :data="chart.monthlyCostData.value" :options="chart.commonBarOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別採用数推移</v-card-title>
            <v-card-text>
              <GenericChart type="bar" :data="chart.monthlyHiresData.value" :options="chart.commonBarOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12">
          <v-card>
            <v-card-title>月別単価推移</v-card-title>
            <v-card-text>
              <GenericChart type="line" :data="chart.monthlyUnitPriceData.value" :options="chart.lineOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>媒体別コスト比較</v-card-title>
            <v-card-text>
              <GenericChart type="bar" :data="chart.mediaCostData.value" :options="chart.horizontalBarOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>媒体別採用数比較</v-card-title>
            <v-card-text>
              <GenericChart type="bar" :data="chart.mediaHiresData.value" :options="chart.horizontalBarOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12">
          <v-card>
            <v-card-title>媒体別単価比較</v-card-title>
            <v-card-text>
              <GenericChart type="bar" :data="chart.mediaUnitPriceData.value" :options="chart.horizontalBarOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>コスト構成比</v-card-title>
            <v-card-text>
              <GenericChart type="doughnut" :data="chart.costShareData.value" :options="chart.doughnutOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>採用数構成比</v-card-title>
            <v-card-text>
              <GenericChart type="pie" :data="chart.hiresShareData.value" :options="chart.pieOptions.value" />
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>