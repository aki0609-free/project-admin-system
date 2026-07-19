import type { Meta, StoryObj } from '@storybook/vue3'
import ToolbarLayout from './ToolbarLayout.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'

const meta: Meta<typeof ToolbarLayout> = {
  title: 'Layout/ToolbarLayout',
  component: ToolbarLayout,
}

export default meta
type Story = StoryObj<typeof ToolbarLayout>

export const BasicPage: Story = {
  render: () => ({
    components: { ToolbarLayout, GenericToolbar },
    setup() {
      const items = [
        { type: 'button', label: '保存', onClick: () => alert('保存') },
      ]
      return { items }
    },
    template: `
      <div style="height:600px">
        <ToolbarLayout>
          <template #toolbar>
            <GenericToolbar :items="items" />
          </template>

          <div style="height:1000px">
            長いコンテンツ
          </div>
        </ToolbarLayout>
      </div>
    `,
  }),
}