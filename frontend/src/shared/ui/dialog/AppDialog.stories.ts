import { ref } from 'vue'
import type { Meta, StoryObj } from '@storybook/vue3-vite'
import AppDialog from './AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

const meta = {
  title: 'Shared UI/Dialog/AppDialog',
  component: AppDialog,
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof AppDialog>

export default meta
type Story = StoryObj<typeof meta>

export const Standard: Story = {
  args: {
    modelValue: true,
    title: '共通Dialog',
  },
  render: () => ({
    components: { AppDialog },
    setup() {
      const open = ref(true)
      const rightFooterItems: ToolbarItem[] = [
        {
          type: 'button',
          label: '閉じる',
          intent: 'secondary',
          onClick: () => {
            open.value = false
          },
        },
        {
          type: 'button',
          label: '保存',
          intent: 'primary',
          onClick: () => undefined,
        },
      ]

      return { open, rightFooterItems }
    },
    template: `
      <div class="pa-6">
        <v-btn @click="open = true">Dialogを開く</v-btn>
        <AppDialog
          v-model="open"
          title="共通Dialog"
          size="md"
          body-layout="stack"
          :right-footer-items="rightFooterItems"
        >
          <v-text-field label="名称" />
          <v-textarea label="備考" rows="4" />
        </AppDialog>
      </div>
    `,
  }),
}
