import type { Meta, StoryObj } from '@storybook/vue3'
import { ref } from 'vue'
import BaseDialog from './BaseDialog.vue'
import { VBtn, VTextField } from 'vuetify/components'

const meta: Meta<typeof BaseDialog> = {
  title: 'Components/Dialogs/Dialog',
  component: BaseDialog,
}

export default meta
type Story = StoryObj<typeof BaseDialog>

export const Default: Story = {
  render: () => ({
    components: { BaseDialog, VBtn, VTextField },
    setup() {
      const open = ref(false)
      const formModel = ref({ name: '', email: '' })
      const submit = () => alert(JSON.stringify(formModel.value, null, 2))
      return { open, formModel, submit }
    },
    template: `
      <div>
        <v-btn color="primary" @click="open = true">Open Dialog</v-btn>

        <BaseDialog v-model="open" title="フォームダイアログ">
          <v-text-field
            v-model="formModel.name"
            label="名前"
          />
          <v-text-field
            v-model="formModel.email"
            label="メール"
          />
          <template #actions>
            <v-btn color="primary" @click="submit">保存</v-btn>
          </template>
        </BaseDialog>
      </div>
    `,
  }),
}