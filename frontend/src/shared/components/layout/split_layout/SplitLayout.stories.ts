import type { Meta, StoryObj } from '@storybook/vue3'
import SplitLayout from './SplitLayout.vue'
import CardLayout from '../card_layout/CardLayout.vue'

const meta: Meta<typeof SplitLayout> = {
  title: 'Layout/SplitLayout',
  component: SplitLayout,
}

export default meta
type Story = StoryObj<typeof SplitLayout>

export const Horizontal: Story = {
  render: () => ({
    components: { SplitLayout, CardLayout },
    template: `
    <div style="height:500px">
      <SplitLayout :ratio="25">
        <template #first>
          フィルター
        </template>

        <template #second>
          メインコンテンツ
        </template>
      </SplitLayout>
    </div>
    `,
  }),
}

export const Vertical: Story = {
  render: () => ({
    components: { SplitLayout, CardLayout },
    template: `
    <div style="height:500px">
      <SplitLayout direction="vertical" :fixed="180">
        <template #first>
          上部フィルター
        </template>
    
        <template #second>
          テーブル / チャート
        </template>
      </SplitLayout>
    </div>
    `,
  }),
}