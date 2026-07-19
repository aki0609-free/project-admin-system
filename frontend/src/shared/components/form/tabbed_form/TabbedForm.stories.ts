import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import TabbedForm from './TabbedForm.vue'
import FormLayout from '../base/FormLayout.vue'
import type { TabbedFormFieldDef } from '@/shared/components/form/tabbed_form/types/types'
import { VBtn } from 'vuetify/components'

interface DemoForm {
  name: string
  age: number
  role: string
  birthday: string
}

const meta: Meta<typeof TabbedForm> = {
  title: 'Components/Forms/TabbedForm',
}

export default meta
type Story = StoryObj<typeof TabbedForm>

// ----------------------
// タブ
// ----------------------
const tabs = ['基本情報', '設定']

// ----------------------
// フィールド
// ----------------------
const fields: TabbedFormFieldDef<DemoForm>[] = [
  { key: 'name', label: '名前', type: 'text', tab: '基本情報' },
  { key: 'age', label: '年齢', type: 'number', tab: '基本情報' },
  { key: 'birthday', label: '誕生日', type: 'date', tab: '基本情報' },
  {
    key: 'role',
    label: '役割',
    type: 'select',
    tab: '設定',
    options: [
      { title: '管理者', value: 'admin' },
      { title: '一般', value: 'user' },
    ],
  },
]

// ----------------------
// Zod Schema（簡易）
// ----------------------
const schema = z.object({
  name: z.string().min(1, '必須項目です'),
  age: z.number().min(0, '正の数を入力してください'),
  birthday: z.string(),
  role: z.string(),
})

export const Default: Story = {
  render: () => ({
    components: { TabbedForm, FormLayout, VBtn },
    setup() {
      const formModel = reactive<DemoForm>({
        name: '',
        age: 0,
        birthday: '',
        role: 'user',
      })

      const submit = () => {
        alert(JSON.stringify(formModel, null, 2))
      }

      return {
        formModel,
        tabs,
        fields,
        schema,
        submit,
      }
    },
    template: `
      <FormLayout
        v-model="formModel"
        :schema="schema"
      >
        <TabbedForm
          v-model="formModel"
          :tabs="tabs"
          :fields="fields"
        />
      </FormLayout>
    `,
  }),
}