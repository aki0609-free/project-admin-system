import { computed, type ComputedRef } from 'vue'
import type { ChartData, ChartOptions } from 'chart.js'
import { formatYearMonth } from '@/shared/utils/DateUtils'
import { generateColors } from '@/shared/utils/UiUtils'

type MonthlySummaryItem = {
  mediaYearMonth: string
  cost: number
  hires: number
  unitPrice: number
}

type MediaSummaryItem = {
  mediaName: string
  cost: number
  hires: number
  unitPrice: number
}

export const useApplicationMediaChartData = (
  monthlySummary: ComputedRef<MonthlySummaryItem[]>,
  mediaSummary: ComputedRef<MediaSummaryItem[]>,
) => {
  const monthlyCostData = computed<ChartData<'bar'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.mediaYearMonth)),
    datasets: [
      {
        label: 'コスト',
        data: monthlySummary.value.map(item => item.cost),
        backgroundColor: '#D3DEF1',
      },
    ],
  }))

  const monthlyHiresData = computed<ChartData<'bar'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.mediaYearMonth)),
    datasets: [
      {
        label: '採用数',
        data: monthlySummary.value.map(item => item.hires),
        backgroundColor: '#D3DEF1',
      },
    ],
  }))

  const monthlyUnitPriceData = computed<ChartData<'line'>>(() => ({
    labels: monthlySummary.value.map(item => formatYearMonth(item.mediaYearMonth)),
    datasets: [
      {
        label: '単価',
        data: monthlySummary.value.map(item => item.unitPrice),
        backgroundColor: '#D3DEF1',
        borderColor: '#D3DEF1',
      },
    ],
  }))

  const mediaCostData = computed<ChartData<'bar'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [
      {
        label: '媒体別コスト',
        data: mediaSummary.value.map(item => item.cost),
        backgroundColor: '#D3DEF1',
      },
    ],
  }))

  const mediaHiresData = computed<ChartData<'bar'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [
      {
        label: '媒体別採用数',
        data: mediaSummary.value.map(item => item.hires),
        backgroundColor: '#D3DEF1',
      },
    ],
  }))

  const mediaUnitPriceData = computed<ChartData<'bar'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [
      {
        label: '媒体別単価',
        data: mediaSummary.value.map(item => item.unitPrice),
        backgroundColor: '#D3DEF1',
      },
    ],
  }))

  const costShareData = computed<ChartData<'doughnut'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [
      {
        label: 'コスト構成比',
        data: mediaSummary.value.map(item => item.cost),
        backgroundColor: generateColors(mediaSummary.value.length),
      },
    ],
  }))

  const hiresShareData = computed<ChartData<'pie'>>(() => ({
    labels: mediaSummary.value.map(item => item.mediaName),
    datasets: [
      {
        label: '採用数構成比',
        data: mediaSummary.value.map(item => item.hires),
        backgroundColor: generateColors(mediaSummary.value.length),
      },
    ],
  }))

  const commonBarOptions = computed<ChartOptions<'bar'>>(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true },
    },
  }))

  const lineOptions = computed<ChartOptions<'line'>>(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true },
    },
  }))

  const horizontalBarOptions = computed<ChartOptions<'bar'>>(() => ({
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: {
      legend: { display: true },
    },
  }))

  const pieOptions = computed<ChartOptions<'pie'>>(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom' },
    },
  }))

  const doughnutOptions = computed<ChartOptions<'doughnut'>>(() => ({
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom' },
    },
  }))

  return {
    monthlyCostData,
    monthlyHiresData,
    monthlyUnitPriceData,
    mediaCostData,
    mediaHiresData,
    mediaUnitPriceData,
    costShareData,
    hiresShareData,
    commonBarOptions,
    lineOptions,
    horizontalBarOptions,
    pieOptions,
    doughnutOptions,
  }
}