import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import NestedRepeatableForm from './NestedRepeatableForm.vue'
import NestedFormLayout from '../base/NestedFormLayout.vue'
import { FormFieldDef } from '../base/types/types'

interface Address {
  city: string
  zip: string
}

interface DemoForm {
  name: string
  addresses: Address[]
}

const schema = z.object({
  name: z.string().min(1, '名前は必須です'),
  addresses: z.array(
    z.object({
      city: z.string().min(1, '市区町村は必須です'),
      zip: z.string().min(1, '郵便番号は必須です'),
    })
  ),
})

const addressFields: FormFieldDef<any>[] = [
  {
    key: 'city',
    label: '市区町村',
    type: 'text',
    required: true,
  },
  {
    key: 'zip',
    label: '郵便番号',
    type: 'text',
    required: true,
  },
]

const meta: Meta<typeof NestedRepeatableForm> = {
  title: 'Components/Forms/NestedRepeatableForm',
}

export default meta
type Story = StoryObj<typeof NestedRepeatableForm>

export const Default: Story = {
  render: () => ({
    components: { NestedFormLayout, NestedRepeatableForm },
    setup() {
      const model = reactive<DemoForm>({
        name: '',
        addresses: [
          {
            city: '',
            zip: '',
          },
        ],
      })

      return {
        model,
        schema,
        addressFields,
      }
    },
    template: `
      <NestedFormLayout
        v-model="model"
        :schema="schema"
      >
        <NestedRepeatableForm
          name="addresses"
          :fields="addressFields"
        />
      </NestedFormLayout>
    `,
  }),
}