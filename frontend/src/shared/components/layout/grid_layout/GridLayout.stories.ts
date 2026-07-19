import type { Meta, StoryObj } from '@storybook/vue3'
import GridLayout from './GridLayout.vue'
import CardLayout from '../card_layout/CardLayout.vue'

const meta: Meta<typeof GridLayout> = {
  title: 'Layout/GridLayout',
  component: GridLayout,
}

export default meta
type Story = StoryObj<typeof GridLayout>

export const Dashboard: Story = {
  render: () => ({
    components: { GridLayout, CardLayout },
    template: `
      <GridLayout
        :items="[
          { cols: 12, md: 6 },
          { cols: 12, md: 6 },
          { cols: 12 }
        ]"
      >
        <template #item-0>
          <CardLayout title="売上">
            売上チャート
          </CardLayout>
        </template>

        <template #item-1>
          <CardLayout title="ユーザー数">
            ユーザー数チャート
          </CardLayout>
        </template>

        <template #item-2>
          <CardLayout title="テーブル">
            データテーブル
          </CardLayout>
        </template>
      </GridLayout>
    `,
  }),
}