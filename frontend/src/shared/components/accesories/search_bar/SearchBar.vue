<script setup lang="ts" generic="T extends Record<string, any>">
import { ref } from 'vue'
import { VBtn } from 'vuetify/components'
import EditableFormCell from '@/shared/components/form/base/EditableFormCell.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import { FormFieldDef } from '@/shared/components/form/base/types/types'
import z from 'zod'

// props
const props = defineProps<{
  fields: FormFieldDef<T>[]
}>()

const emit = defineEmits<{
  (e: 'search', value: T): void
}>()

// フォーム用モデル
const model = ref<Partial<T>>({})
const emptyScheme = z.object({})

// 検索
const onSearch = () => {
  emit('search', model.value)
}

// クリア
const onClear = () => {
  Object.keys(model).forEach((k) => {
    delete model[k as keyof typeof model]
  })
}
</script>

<template>
  <FormLayout v-model="model as any" :schema=emptyScheme>
      <v-row dense>
        <v-col
          v-for="field in fields"
          :key="String(field.key)"
          cols="12"
          md="4"
          pa="2"
        >
          <EditableFormCell :field="field" />
        </v-col>
      </v-row>

      <div class="d-flex justify-end mt-2 ga-2">
        <VBtn variant="outlined" @click="onClear">
          クリア
        </VBtn>
        <VBtn color="primary" @click="onSearch">
          検索
        </VBtn>
      </div>
  </FormLayout>
</template>