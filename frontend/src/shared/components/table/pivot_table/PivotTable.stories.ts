import type { Meta, StoryObj } from '@storybook/vue3'
import PivotTable from './PivotTable.vue'

const meta: Meta<typeof PivotTable> = {
  title: 'Components/Tables/PivotTable',
  component: PivotTable as any,
}

export default meta
type Story = StoryObj<typeof PivotTable>

const data = [
  { year: '2023', gender: '男', sales: 100 },
  { year: '2023', gender: '女', sales: 120 },
  { year: '2024', gender: '男', sales: 110 },
  { year: '2024', gender: '女', sales: 140 },
]

export const Default: Story = {
  args: {
    data,
    def: {
      row: {
        key: 'year',
        title: '年',
      },
      column: {
        key: 'gender',
        title: '性別',
      },
      values: [
        {
          key: 'sales',
          aggregation: 'sum',
        },
      ],
    },
  },
}