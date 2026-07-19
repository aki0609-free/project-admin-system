import type { Meta, StoryObj } from '@storybook/vue3'
import GenericChart from './GenericChart.vue'
import type { ChartData } from 'chart.js'

const meta: Meta<typeof GenericChart> = {
  title: 'Components/Chart/GenericChart',
  component: GenericChart,
}

export default meta
type Story = StoryObj<typeof GenericChart>

const baseData: ChartData<any> = {
  labels: ['Jan', 'Feb', 'Mar', 'Apr'],
  datasets: [
    {
      label: '売上',
      data: [10, 20, 15, 30],
      backgroundColor: '#1976d2',
    },
  ],
}

export const Bar: Story = {
  args: {
    type: 'bar',
    data: baseData,
  },
}

export const Line: Story = {
  args: {
    type: 'line',
    data: baseData,
  },
}

export const Pie: Story = {
  args: {
    type: 'pie',
    data: {
      labels: ['A', 'B', 'C'],
      datasets: [
        {
          data: [30, 40, 30],
          backgroundColor: ['#1976d2', '#42a5f5', '#90caf9'],
        },
      ],
    },
  },
}

export const Doughnut: Story = {
  args: {
    type: 'doughnut',
    data: {
      labels: ['A', 'B', 'C'],
      datasets: [
        {
          data: [25, 50, 25],
          backgroundColor: ['#ff9800', '#f44336', '#4caf50'],
        },
      ],
    },
  },
}