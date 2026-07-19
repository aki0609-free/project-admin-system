import type { Meta, StoryObj } from '@storybook/vue3'
import CardLayout from './CardLayout.vue'
import { VBtn } from 'vuetify/components'

const meta: Meta<typeof CardLayout> = {
  title: 'Layout/CardLayout',
  component: CardLayout,
}

export default meta
type Story = StoryObj<typeof CardLayout>

export const Default: Story = {
  args: {
    title: '売上サマリー',
    subtitle: '2024年データ',
  },
  render: (args) => ({
    components: { CardLayout },
    setup() {
      return { args }
    },
    template: `
      <CardLayout v-bind="args">
        <p>ここにコンテンツが入ります。</p>
      </CardLayout>
    `,
  }),
}

export const WithActions: Story = {
  render: () => ({
    components: { CardLayout, VBtn },
    template: `
      <CardLayout title="アクション付きカード">
        <p>フォームやチャートをここに配置できます。</p>

        <template #actions>
          <v-spacer />
          <v-btn variant="text">キャンセル</v-btn>
          <v-btn color="primary">保存</v-btn>
        </template>
      </CardLayout>
    `,
  }),
}

export const Loading: Story = {
  args: {
    title: '読み込み中カード',
    loading: true,
  },
  render: (args) => ({
    components: { CardLayout },
    setup() {
      return { args }
    },
    template: `
      <CardLayout v-bind="args">
        <p>データ取得中...</p>
      </CardLayout>
    `,
  }),
}