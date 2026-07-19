import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import SectionedForm from './SectionedForm.vue'
import FormLayout from '../base/FormLayout.vue'
import type { FormSection } from './types/types'

interface DemoForm {
  id: number
  name: string
  age: number
  gender: string
  subscribe: boolean
  email: string
}

const meta = {
  title: 'Components/Forms/SectionedForm',
} satisfies Meta<typeof SectionedForm>

export default meta
type Story = StoryObj<typeof meta>

// ----------------------
// Sections
// ----------------------
const sections: FormSection<DemoForm>[] = [
  {
    title: '基本情報',
    fields: [
      { key: 'id', label: 'ID', type: 'number' },
      { key: 'name', label: '名前', type: 'text' },
      { key: 'age', label: '年齢', type: 'number' },
    ],
  },
  {
    title: 'その他情報',
    fields: [
      {
        key: 'gender',
        label: '性別',
        type: 'select',
        options: [
          { title: '男性', value: 'M' },
          { title: '女性', value: 'F' },
        ],
      },
      { key: 'subscribe', label: '購読', type: 'checkbox' },
      { key: 'email', label: 'メール', type: 'text' },
    ],
  },
]

// ----------------------
// Zod Schema
// ----------------------
const schema = z.object({
  id: z.number(),
  name: z.string().min(1, '必須項目です'),
  age: z.number(),
  gender: z.string().min(1, '必須項目です'),
  subscribe: z.boolean(),
  email: z.string().email('メール形式が正しくありません'),
})

export const Default: Story = {
  render: () => ({
    components: { SectionedForm, FormLayout },
    setup() {
      const formModel = reactive<DemoForm>({
        id: 1,
        name: '',
        age: 25,
        gender: '',
        subscribe: false,
        email: '',
      })

      return {
        formModel,
        sections,
        schema,
      }
    },
    template: `
      <FormLayout
        v-model="formModel"
        :schema="schema"
      >
        <SectionedForm
          :sections="sections"
        />
      </FormLayout>
    `,
  }),
}