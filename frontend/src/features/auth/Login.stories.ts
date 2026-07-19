import type { Meta, StoryObj } from '@storybook/vue3'
import Login from './Login.vue'

const meta: Meta<typeof Login> = {
  title: 'Features/Auth/Login',
  component: Login,
  parameters: {
    layout: 'fullscreen',
  },
}

export default meta
type Story = StoryObj<typeof Login>

export const Default: Story = {}

export const WithError: Story = {
  render: () => ({
    components: { Login },
    template: '<Login />',
    mounted() {
      // デモ用にエラー表示
      const vm = this.$.subTree.component?.exposed
      if (vm?.errorMessage !== undefined) {
        vm.errorMessage = 'ログインに失敗しました'
      }
    },
  }),
}