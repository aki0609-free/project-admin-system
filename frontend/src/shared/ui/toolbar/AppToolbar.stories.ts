import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppToolbar from './AppToolbar.vue'
import type { ToolbarItem } from './types'

const meta = {
  title: 'Shared UI/Toolbar/AppToolbar',
  component: AppToolbar,
  parameters: { layout: 'padded' },
} satisfies Meta<typeof AppToolbar>

export default meta
type Story = StoryObj<typeof meta>

const leftItems: ToolbarItem[] = [
  {
    type: 'button',
    label: '新規作成',
    intent: 'primary',
    onClick: () => undefined,
  },
  {
    type: 'search',
    modelValue: '',
    placeholder: '検索',
    'onUpdate:modelValue': () => undefined,
  },
]

const rightItems: ToolbarItem[] = [
  {
    type: 'button',
    label: '再読込',
    intent: 'utility',
    onClick: () => undefined,
  },
  {
    type: 'button',
    label: '削除',
    intent: 'danger',
    onClick: () => undefined,
  },
]

export const PageToolbar: Story = {
  args: {
    leftItems,
    rightItems,
    surface: 'page',
  },
}

export const NarrowWidth: Story = {
  args: {
    leftItems,
    rightItems,
    surface: 'page',
  },
  decorators: [
    () => ({
      template: '<div style="max-width: 520px"><story /></div>',
    }),
  ],
}
