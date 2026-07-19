<script setup lang="ts">
import { toRef } from 'vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import GenericChart from '@/shared/components/chart/GenericChart.vue'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'
import { useApplicantChart } from '@/features/application/composables/applicant/useApplicantChart'

const props = defineProps<{
  applicants: ApplicantRow[]
}>()

const chart = useApplicantChart(toRef(props, 'applicants'))
</script>

<template>
  <div class="d-flex flex-column ga-4">
    <GenericToolbar :items="chart.toolbarItems.value" />

    <v-container fluid class="pa-0">
      <v-row>
        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別応募数</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.monthlyApplicantCountData.value"
                :options="chart.barOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別面接実施数</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.monthlyInterviewCountData.value"
                :options="chart.barOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別在籍中人数</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.monthlyWorkingCountData.value"
                :options="chart.barOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別退職人数</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.monthlyResignedCountData.value"
                :options="chart.barOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別面接実施率</v-card-title>
            <v-card-text>
              <GenericChart
                type="line"
                :data="chart.monthlyInterviewRateData.value"
                :options="chart.lineOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>月別在籍率</v-card-title>
            <v-card-text>
              <GenericChart
                type="line"
                :data="chart.monthlyWorkingRateData.value"
                :options="chart.lineOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>媒体別応募数</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.mediaApplicantCountData.value"
                :options="chart.horizontalBarOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card>
            <v-card-title>媒体別在籍中人数</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.mediaWorkingCountData.value"
                :options="chart.horizontalBarOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12">
          <v-card>
            <v-card-title>媒体別退職人数</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.mediaResignedCountData.value"
                :options="chart.horizontalBarOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12">
          <v-card>
            <v-card-title>媒体別在籍率</v-card-title>
            <v-card-text>
              <GenericChart
                type="bar"
                :data="chart.mediaWorkingRateData.value"
                :options="chart.horizontalBarOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="4">
          <v-card>
            <v-card-title>ステータス内訳</v-card-title>
            <v-card-text>
              <GenericChart
                type="pie"
                :data="chart.statusCountData.value"
                :options="chart.pieOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="4">
          <v-card>
            <v-card-title>性別内訳</v-card-title>
            <v-card-text>
              <GenericChart
                type="doughnut"
                :data="chart.genderCountData.value"
                :options="chart.doughnutOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="4">
          <v-card>
            <v-card-title>雇用形態内訳</v-card-title>
            <v-card-text>
              <GenericChart
                type="pie"
                :data="chart.contractTypeCountData.value"
                :options="chart.pieOptions.value"
              />
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>