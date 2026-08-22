<script setup lang="ts">
import { computed, ref, toRef } from 'vue'

import { useNoticeEditDialog } from '@/features/dashboard/composables/useNoticeEditDialog'
import type {
  NoticeCreateRequest,
  NoticeResponse,
} from '@/features/dashboard/types/dashboardTypes'
import NoticeContentPreview from '@/shared/components/notice/NoticeContentPreview.vue'
import NoticeRichEditor from '@/shared/components/notice/NoticeRichEditor.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'

const props = defineProps<{
  modelValue: boolean
  notice?: NoticeResponse | null
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [value: NoticeCreateRequest]
}>()

const dialog = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const formLayout = ref<{ validateAll: () => boolean } | null>(null)

const {
  form,
  activeTab,
  isEdit,
  fields,
  schema,
  rightFooterItems,
} = useNoticeEditDialog(
  dialog,
  toRef(props, 'notice'),
  value => {
    if (!formLayout.value?.validateAll()) return
    emit('submit', value)
  },
  computed(() => props.saving ?? false),
)
</script>

<template>
  <AppDialog
    v-model="dialog"
    :title="isEdit ? 'お知らせ編集' : 'お知らせ作成'"
    size="lg"
    :max-width="920"
    body-layout="stack"
    :right-footer-items="rightFooterItems"
  >
    <FormLayout ref="formLayout" v-model="form" :schema="schema">
      <GridBasedForm v-model="form" :fields="fields" />
    </FormLayout>

    <div class="notice-content-editor">
      <v-tabs v-model="activeTab" density="compact">
        <v-tab value="edit">
          編集
        </v-tab>

        <v-tab value="preview">
          プレビュー
        </v-tab>
      </v-tabs>

      <v-window v-model="activeTab">
        <v-window-item value="edit">
          <div class="mt-4">
            <NoticeRichEditor
              v-if="form.contentFormat === 'HTML'"
              v-model="form.content"
            />

            <v-textarea
              v-else
              v-model="form.content"
              label="内容"
              variant="outlined"
              rows="8"
              auto-grow
            />
          </div>
        </v-window-item>

        <v-window-item value="preview">
          <NoticeContentPreview
            class="mt-4"
            :content="form.content"
            :content-format="form.contentFormat"
          />
        </v-window-item>
      </v-window>
    </div>
  </AppDialog>
</template>

<style scoped>
.notice-content-editor {
  display: grid;
  gap: 8px;
  min-width: 0;
}
</style>
