<script setup lang="ts">
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import { useMailTestSendTab } from '@/features/system/mail/composables/useMailTestSendTab'

const {
  formModel,
  fields,
  schema,
  lastMessage,
  lastMessageType,
  leftToolbarItems,
} = useMailTestSendTab()
</script>

<template>
  <div class="mail-tab">
    <div class="tab-header">
      <h3>テスト送信</h3>
      <p class="tab-description">
        mail_send_queue にテストメールを登録し、既存の MailSendService 経由で送信します。
      </p>
    </div>

    <v-card variant="outlined" rounded="lg">
      <v-card-text class="form-card">
        <FormLayout v-model="formModel" :schema="schema">
          <GridBasedForm
            v-model="formModel"
            :fields="fields"
          />
        </FormLayout>

        <AppToolbar :left-items="leftToolbarItems" surface="plain" />
      </v-card-text>
    </v-card>

    <v-alert
      v-if="lastMessage"
      :type="lastMessageType"
      variant="tonal"
    >
      {{ lastMessage }}
    </v-alert>
  </div>
</template>

<style scoped>
.mail-tab {
  display: grid;
  gap: 12px;
}

.tab-header {
  display: grid;
  gap: 6px;
}

.tab-description {
  margin: 0;
  color: #64748b;
}

.form-card {
  display: grid;
  gap: 16px;
}

</style>
