import type { Meta, StoryObj } from '@storybook/vue3'
import ErrorPage from './ErrorPage.vue'

const meta: Meta<typeof ErrorPage> = {
  title: 'Components/Errors/ErrorPage',
  component: ErrorPage,
}

export default meta
type Story = StoryObj<typeof ErrorPage>

export const Default: Story = {
  render: () => ({
    components: { ErrorPage },
    template: `<ErrorPage />`,
  }),
}

export const WithRetry: Story = {
  render: () => ({
    components: { ErrorPage },
    setup() {
      const retry = () => alert('再試行ボタンが押されました')
      return { retry }
    },
    template: `<ErrorPage 
                 title="読み込みエラー" 
                 message="ネットワーク接続を確認してください"
                 :retry="retry"
               />`,
  }),
}