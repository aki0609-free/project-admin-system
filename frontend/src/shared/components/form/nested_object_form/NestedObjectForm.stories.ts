import type { Meta, StoryObj } from '@storybook/vue3'
import { reactive } from 'vue'
import { z } from 'zod'
import NestedObjectForm from './NestedObjectForm.vue'
import NestedFormLayout from '../base/NestedFormLayout.vue'
import type { NestedObjectFormFieldDef } from './types/types'

interface Address {
  city: string
  zip: string
}

interface UserProfile {
  name: string
  age: number
  address: Address
  previousAddresses: Address[]
}

const schema = z.object({
  name: z.string().min(1, '名前は必須です'),
  age: z.number().min(0, '年齢は0以上で入力してください'),
  address: z.object({
    city: z.string().min(1, '市区町村は必須です'),
    zip: z.string().min(1, '郵便番号は必須です'),
  }),
  previousAddresses: z.array(
    z.object({
      city: z.string().min(1),
      zip: z.string().min(1),
    })
  ),
})

const fields: NestedObjectFormFieldDef<UserProfile>[] = [
  { key: 'name', label: '名前', type: 'text', required: true },
  { key: 'age', label: '年齢', type: 'number', required: true },
  {
    key: 'address',
    label: '住所',
    type: 'object',
    fields: [
      { key: 'city', label: '市区町村', type: 'text', required: true },
      { key: 'zip', label: '郵便番号', type: 'text', required: true },
    ],
  },
  {
    key: 'previousAddresses',
    label: '過去の住所',
    type: 'array',
    itemFields: [
      { key: 'city', label: '市区町村', type: 'text', required: true },
      { key: 'zip', label: '郵便番号', type: 'text', required: true },
    ],
  },
]

const meta: Meta<typeof NestedObjectForm> = {
  title: 'Components/Forms/NestedObjectForm',
}

export default meta
type Story = StoryObj<typeof NestedObjectForm>

export const Default: Story = {
  render: () => ({
    components: { NestedObjectForm, NestedFormLayout },
    setup() {
      const model = reactive<UserProfile>({
        name: '',
        age: 0,
        address: { city: '', zip: '' },
        previousAddresses: [
          { city: '', zip: '' },
        ],
      })

      return {
        model,
        schema,
        fields,
      }
    },
    template: `
      <NestedFormLayout v-model="model" :schema="schema">
        <NestedObjectForm
          :fields="fields"
        />
      </NestedFormLayout>
    `,
  }),
}