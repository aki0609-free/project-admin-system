import type { Meta, StoryObj } from '@storybook/vue3'
import { ref } from 'vue'
import TabLayout from './TabLayout.vue'
import { useRouteTab } from './composables/useRouteTab'

const meta: Meta<typeof TabLayout> = {
  title: 'Layout/TabLayout',
  component: TabLayout,
}

export default meta
type Story = StoryObj<typeof TabLayout>

export const Basic: Story = {
  render: () => ({
    components: { TabLayout },
    setup() {
      const tabs = [
        { label: '概要', value: 'summary' },
        { label: '詳細', value: 'detail' },
        { label: '推移', value: 'trend' },
      ]

      const active = ref('summary')
      const { activeTab } = useRouteTab('summary', active)

      return { tabs, activeTab }
    },
    template: `
      <TabLayout v-model="activeTab" :tabs="tabs">
        <template #default="{ active }">
          <div v-if="active === 'summary'">概要コンテンツ</div>
          <div v-else-if="active === 'detail'">詳細コンテンツ</div>
          <div v-else-if="active === 'trend'">推移コンテンツ</div>
        </template>
      </TabLayout>
    `,
  }),
}