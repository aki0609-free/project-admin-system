<script setup lang="ts">
import { SupportedChartType } from '@/shared/components/chart/types/types';
import { componentMap } from '@/shared/components/chart/utils/componentMap';
import { ArcElement, BarElement, CategoryScale, ChartData, Chart as ChartJS, ChartOptions, ChartType, Legend, LinearScale, LineElement, PointElement, Title, Tooltip } from 'chart.js'
import { computed } from 'vue';

ChartJS.register(
    Title,
    Tooltip,
    Legend,
    CategoryScale,
    LinearScale,
    BarElement,
    LineElement,
    ArcElement,
    PointElement
)

const props = defineProps<{
    type: SupportedChartType
    data: ChartData<any>
    options?: ChartOptions<any>
}>()

const chartComponent = computed(() => componentMap[props.type])
</script>

<template>
  <div style="height: 400px">
    <component
      :is="chartComponent"
      :data="data"
      :options="options"
    />
  </div>
</template>