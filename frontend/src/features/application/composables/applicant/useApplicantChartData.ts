import { computed, type Ref } from 'vue'
import type { ChartData } from 'chart.js'
import { formatYearMonth } from '@/shared/utils/DateUtils'
import { generateColors } from '@/shared/utils/UiUtils'

const BASE_COLOR = '#D3DEF1'

type MonthlySummaryRow = {
  yearMonth: string
  applicants: number
  interviewed: number
  working: number
  resigned: number
  interviewRate: number
  workingRate: number
}

type MediaSummaryRow = {
  mediaName: string
  applicants: number
  working: number
  resigned: number
  workingRate: number
}

type RatioSummaryRow = {
  label: string
  count: number
}

export const useApplicantChartData = (
  monthlySummary: Ref<MonthlySummaryRow[]>,
  mediaSummary: Ref<MediaSummaryRow[]>,
  statusSummary: Ref<RatioSummaryRow[]>,
  genderSummary: Ref<RatioSummaryRow[]>,
  contractTypeSummary: Ref<RatioSummaryRow[]>,
) => {
  const monthlyApplicantCountData = computed<ChartData<'bar'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.yearMonth)),
    datasets: [{ label: '応募数', data: monthlySummary.value.map(item => item.applicants), backgroundColor: BASE_COLOR }],
  }))

  const monthlyInterviewCountData = computed<ChartData<'bar'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.yearMonth)),
    datasets: [{ label: '面接実施数', data: monthlySummary.value.map(item => item.interviewed), backgroundColor: BASE_COLOR }],
  }))

  const monthlyWorkingCountData = computed<ChartData<'bar'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.yearMonth)),
    datasets: [{ label: '在籍中人数', data: monthlySummary.value.map(item => item.working), backgroundColor: BASE_COLOR }],
  }))

  const monthlyResignedCountData = computed<ChartData<'bar'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.yearMonth)),
    datasets: [{ label: '退職人数', data: monthlySummary.value.map(item => item.resigned), backgroundColor: BASE_COLOR }],
  }))

  const monthlyInterviewRateData = computed<ChartData<'line'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.yearMonth)),
    datasets: [{ label: '面接実施率(%)', data: monthlySummary.value.map(item => item.interviewRate), backgroundColor: BASE_COLOR, borderColor: BASE_COLOR }],
  }))

  const monthlyWorkingRateData = computed<ChartData<'line'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.yearMonth)),
    datasets: [{ label: '在籍率(%)', data: monthlySummary.value.map(item => item.workingRate), backgroundColor: BASE_COLOR, borderColor: BASE_COLOR }],
  }))

  const mediaApplicantCountData = computed<ChartData<'bar'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [{ label: '媒体別応募数', data: mediaSummary.value.map(item => item.applicants), backgroundColor: BASE_COLOR }],
  }))

  const mediaWorkingCountData = computed<ChartData<'bar'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [{ label: '媒体別在籍中人数', data: mediaSummary.value.map(item => item.working), backgroundColor: BASE_COLOR }],
  }))

  const mediaResignedCountData = computed<ChartData<'bar'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [{ label: '媒体別退職人数', data: mediaSummary.value.map(item => item.resigned), backgroundColor: BASE_COLOR }],
  }))

  const mediaWorkingRateData = computed<ChartData<'bar'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [{ label: '媒体別在籍率(%)', data: mediaSummary.value.map(item => item.workingRate), backgroundColor: BASE_COLOR }],
  }))

  const statusCountData = computed<ChartData<'pie'>>(() => ({
    labels: statusSummary.value.map(item => item.label),
    datasets: [{ label: 'ステータス内訳', data: statusSummary.value.map(item => item.count), backgroundColor: generateColors(statusSummary.value.length) }],
  }))

  const genderCountData = computed<ChartData<'doughnut'>>(() => ({
    labels: genderSummary.value.map(item => item.label),
    datasets: [{ label: '性別内訳', data: genderSummary.value.map(item => item.count), backgroundColor: generateColors(genderSummary.value.length) }],
  }))

  const contractTypeCountData = computed<ChartData<'pie'>>(() => ({
    labels: contractTypeSummary.value.map(item => item.label),
    datasets: [{ label: '雇用形態内訳', data: contractTypeSummary.value.map(item => item.count), backgroundColor: generateColors(contractTypeSummary.value.length) }],
  }))

  return {
    monthlyApplicantCountData,
    monthlyInterviewCountData,
    monthlyWorkingCountData,
    monthlyResignedCountData,
    monthlyInterviewRateData,
    monthlyWorkingRateData,
    mediaApplicantCountData,
    mediaWorkingCountData,
    mediaResignedCountData,
    mediaWorkingRateData,
    statusCountData,
    genderCountData,
    contractTypeCountData,
  }
}