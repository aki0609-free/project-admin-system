<script setup lang="ts">
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import { useMailTestSendTab } from '@/features/system/mail/composables/useMailTestSendTab'

const {
  formModel,
  fields,
  schema,
  sendMutation,
  lastMessage,
  send,
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

        <div class="actions">
          <v-btn
            color="primary"
            variant="elevated"
            :loading="sendMutation.isPending.value"
            :disabled="sendMutation.isPending.value"
            @click="send"
          >
            テスト送信
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <v-alert
      v-if="lastMessage"
      type="success"
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

.actions {
  display: flex;
  justify-content: flex-end;
}
</style>