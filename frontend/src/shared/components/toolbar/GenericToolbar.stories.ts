import type { Meta, StoryObj } from '@storybook/vue3'
import { ref } from 'vue'
import GenericToolbar from './GenericToolbar.vue'

const meta: Meta<typeof GenericToolbar> = {
  title: 'Components/Toolbar/GenericToolbar',
  component: GenericToolbar,
}

export default meta
type Story = StoryObj<typeof GenericToolbar>

export const BasicButtons: Story = {
  render: () => ({
    components: { GenericToolbar },
    setup() {
      const save = () => alert('保存しました')
      const refresh = () => alert('更新しました')

      const items = [
        { type: 'button', label: '保存', onClick: save },
        { type: 'button', label: '更新', color: 'secondary', onClick: refresh },
      ]

      return { items }
    },
    template: `<GenericToolbar :items="items" />`,
  }),
}

export const WithDropdownAndSearch: Story = {
  render: () => ({
    components: { GenericToolbar },
    setup() {
      const keyword = ref('')

      const items = [
        {
          type: 'dropdown',
          label: '期間',
          options: [
            { label: '今月', value: 'month' },
            { label: '今年', value: 'year' },
          ],
          onSelect: (v: string) => alert(`選択: ${v}`),
        },
        {
          type: 'search',
          modelValue: keyword.value,
          'onUpdate:modelValue': (v: string) => (keyword.value = v),
        },
      ]

      return { items }
    },
    template: `<GenericToolbar :items="items" />`,
  }),
}