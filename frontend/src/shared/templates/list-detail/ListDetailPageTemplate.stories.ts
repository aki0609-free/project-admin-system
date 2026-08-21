import type { Meta, StoryObj } from '@storybook/vue3-vite'
import ListDetailPageTemplate from './ListDetailPageTemplate.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

const meta = {
  title: 'Shared UI/Templates/ListDetailPage',
  component: ListDetailPageTemplate,
  parameters: { layout: 'padded' },
} satisfies Meta<typeof ListDetailPageTemplate>

export default meta
type Story = StoryObj<typeof meta>

const leftToolbarItems: ToolbarItem[] = [
  {
    type: 'button',
    label: '新規追加',
    intent: 'primary',
    onClick: () => undefined,
  },
]

const rightToolbarItems: ToolbarItem[] = [
  {
    type: 'button',
    label: '再読込',
    intent: 'utility',
    onClick: () => undefined,
  },
]

export const Standard: Story = {
  args: {
    title: 'マスター管理',
    description: '一覧から対象を選択して登録・編集します。',
    leftToolbarItems,
    rightToolbarItems,
    contentPadding: true,
  },
  render: args => ({
    components: { ListDetailPageTemplate },
    setup: () => ({ args }),
    template: `
      <ListDetailPageTemplate v-bind="args">
        <v-alert type="info" variant="tonal">一覧・詳細領域</v-alert>
      </ListDetailPageTemplate>
    `,
  }),
}
