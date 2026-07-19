import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import HorizontalForm from './HorizontalForm.vue'
import FormLayout from '../base/FormLayout.vue'
import type { FormFieldDef } from '../base/types/types'

interface DemoForm {
  id: number
  name: string
  age: number
  gender: string
  subscribe: boolean
  email: string
}

const meta = {
  title: 'Components/Forms/HorizontalForm',
} satisfies Meta<typeof HorizontalForm>

export default meta
type Story = StoryObj<typeof meta>

// ----------------------
// Fields
// ----------------------
const fields: FormFieldDef<DemoForm>[] = [
  { key: 'id', label: 'ID', type: 'number' },
  { key: 'name', label: '名前', type: 'text' },
  { key: 'age', label: '年齢', type: 'number' },
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
]

// ----------------------
// Zod Schema
// ----------------------
const schema = z.object({
  id: z.number(),
  name: z.string().min(1, '必須項目です'),
  age: z.number(),
  gender: z.string(),
  subscribe: z.boolean(),
  email: z.string().email('メール形式が不正です'),
})

export const Default: Story = {
  render: () => ({
    components: { FormLayout, HorizontalForm },
    setup() {
      const formModel = reactive<DemoForm>({
        id: 1,
        name: 'Alice',
        age: 25,
        gender: 'F',
        subscribe: true,
        email: 'alice@example.com',
      })

      return {
        formModel,
        fields,
        schema,
      }
    },
    template: `
      <FormLayout
        v-model="formModel"
        :schema="schema"
      >
        <HorizontalForm
          :fields="fields"
          :label-width="140"
        />
      </FormLayout>
    `,
  }),
}