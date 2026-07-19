import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import GridBasedForm from './GridBasedForm.vue'
import FormLayout from '../base/FormLayout.vue'
import type { GridFormFieldDef } from './types/types'

interface DemoForm {
  id: number
  name: string
  age: number
  gender: string
  subscribe: boolean
  email: string
}

const meta: Meta<typeof GridBasedForm> = {
  title: 'Components/Forms/GridBasedForm',
}

export default meta
type Story = StoryObj<typeof GridBasedForm>

// ----------------------
// 初期データ
// ----------------------
const initialModel: DemoForm = {
  id: 1,
  name: 'Alice',
  age: 25,
  gender: 'F',
  subscribe: true,
  email: 'alice@example.com',
}

// ----------------------
// フィールド定義
// ----------------------
const fields: GridFormFieldDef<DemoForm>[] = [
  { key: 'id', label: 'ID', type: 'number', width: 80 },
  { key: 'name', label: '名前', type: 'text', width: 100, gridColumn: '2 / span 2' },
  { key: 'age', label: '年齢', type: 'number', width: 100 },
  {
    key: 'gender',
    label: '性別',
    type: 'select',
    options: [
      { title: '男性', value: 'M' },
      { title: '女性', value: 'F' },
    ],
    width: 120,
  },
  { key: 'subscribe', label: '購読', type: 'checkbox', width: 100 },
  { key: 'email', label: 'メール', type: 'text', width: 250, gridColumn: '1 / span 3' },
]

// ----------------------
// Zod Schema（簡易）
// ----------------------
const schema = z.object({
  id: z.number(),
  name: z.string().min(1, '必須項目です'),
  age: z.number().min(0),
  gender: z.string(),
  subscribe: z.boolean(),
  email: z.string().email(),
})

export const Default: Story = {
  render: () => ({
    components: { GridBasedForm, FormLayout },
    setup() {
      const formModel = reactive<DemoForm>({ ...initialModel })

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
        <GridBasedForm
          v-model="formModel"
          :fields="fields"
        />
      </FormLayout>
    `,
  }),
}