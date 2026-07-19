import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import FormLayout from '../base/FormLayout.vue'
import type { FormFieldDef } from '../base/types/types'
import { DynamicFormFieldDef } from './types/types'
import DynamicForm from './DynamicForm.vue'

interface DemoForm {
  subscribe: boolean
  email: string
}

const schema = z.object({
  subscribe: z.boolean(),
  email: z.string().email().optional(),
})

const fields: DynamicFormFieldDef<DemoForm>[] = [
  {
    key: 'subscribe',
    label: '購読する',
    type: 'checkbox',
  },
  {
    key: 'email',
    label: 'メール',
    type: 'text',
    showIf: model => model.subscribe === true,
  },
]

const meta: Meta<typeof DynamicForm> = {
  title: 'Components/Forms/DynamicForm',
}

export default meta
type Story = StoryObj<typeof DynamicForm>

export const Default: Story = {
  render: () => ({
    components: { DynamicForm, FormLayout },
    setup() {
      const model = reactive<DemoForm>({
        subscribe: false,
        email: '',
      })

      return { model, fields, schema }
    },
    template: `
      <FormLayout
        v-model="model"
        :schema="schema"
      >
        <DynamicForm :fields="fields" />
      </FormLayout>
    `,
  }),
}