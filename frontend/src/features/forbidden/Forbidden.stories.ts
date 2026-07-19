import type { Meta, StoryObj } from '@storybook/vue3'
import Forbidden from './Forbidden.vue'

const meta: Meta<typeof Forbidden> = {
  title: 'Features/System/Forbidden',
  component: Forbidden,
  parameters: {
    layout: 'fullscreen',
  },
}

export default meta
type Story = StoryObj<typeof Forbidden>

export const Default: Story = {}