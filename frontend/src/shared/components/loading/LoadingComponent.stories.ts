import type { Meta, StoryObj } from '@storybook/vue3'
import LoadingComponent from './LoadingComponent.vue'

const meta: Meta<typeof LoadingComponent> = {
  title: 'Components/Loading/LoadingComponent',
  component: LoadingComponent,
}

export default meta
type Story = StoryObj<typeof LoadingComponent>

export const Default: Story = {
  render: () => ({
    components: { LoadingComponent },
    template: `<LoadingComponent />`,
  }),
}

export const CustomTextAndSize: Story = {
  render: () => ({
    components: { LoadingComponent },
    template: `<LoadingComponent text="データ読み込み中…" :size="64" />`,
  }),
}