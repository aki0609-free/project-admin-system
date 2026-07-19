import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import WizardForm, { WizardStep } from './WizardForm.vue'
import FormLayout from '../base/FormLayout.vue'

interface DemoForm {
  name: string
  age: number
  email: string
  password: string
}

const schema = z.object({
  name: z.string().min(1, '名前は必須です'),
  age: z.coerce.number().min(1, '年齢は必須です'),
  email: z.string().email('メール形式が不正です'),
  password: z.string().min(6, '6文字以上必要です'),
})

const meta: Meta<typeof WizardForm> = {
  title: 'Components/Forms/WizardForm',
}

export default meta
type Story = StoryObj<typeof WizardForm>

const steps: WizardStep<DemoForm>[] = [
  {
    title: '基本情報',
    fields: [
      { key: 'name', label: '名前', type: 'text', required: true },
      { key: 'age', label: '年齢', type: 'number', required: true },
    ],
  },
  {
    title: '認証情報',
    fields: [
      { key: 'email', label: 'メール', type: 'text', required: true },
      { key: 'password', label: 'パスワード', type: 'text', required: true },
    ],
  },
]

export const Default: Story = {
  render: () => ({
    components: { WizardForm, FormLayout },
    setup() {
      const model = reactive<DemoForm>({
        name: '',
        age: 0,
        email: '',
        password: '',
      })

      const finish = () => {
        alert('Completed!')
      }

      return {
        model,
        steps,
        finish,
        schema,
      }
    },
    template: `
      <FormLayout
        v-model="model"
        :schema="schema"
      >
        <WizardForm
          :steps="steps"
          @finish="finish"
        />
      </FormLayout>
    `,
  }),
}