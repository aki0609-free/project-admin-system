<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type {
  EnvelopePrintCustomerOption,
  EnvelopePrintPayload,
  EnvelopeType,
} from '@/features/master/customer/types/envelopePrintTypes'

const props = defineProps<{
  modelValue: boolean
  customers: EnvelopePrintCustomerOption[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'print', value: EnvelopePrintPayload): void
}>()

const dialogModel = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})

const form = reactive<EnvelopePrintPayload>({
  customerIds: [],
  envelopeType: 'NAGA3',
  stamp: '請求書在中',
  honorific: '御中',
  fontFamily: 'Yu Gothic',
  fontSize: 16,
})

const envelopeTypeOptions: { title: string; value: EnvelopeType }[] = [
  { title: '長3封筒', value: 'NAGA3' },
  { title: '角2封筒', value: 'KAKU2' },
]

const stampOptions = [
  '請求書在中',
  '見積書在中',
  '納品書在中',
  '重要書類在中',
  '書類在中',
]

const honorificOptions = [
  '御中',
  '様',
  '先生',
  '各位',
  '行',
  '宛',
]

const fontOptions = [
  'Yu Gothic',
  'Yu Mincho',
  'Meiryo',
  'MS Gothic',
  'MS Mincho',
]

const selectedPreviewCustomer = computed(() => {
  const id = form.customerIds[0]
  return props.customers.find(customer => customer.id === id) ?? null
})

const previewName = computed(() => {
  const customer = selectedPreviewCustomer.value

  if (!customer) {
    return `株式会社サンプル　${form.honorific}`
  }

  return `${customer.name}　${form.honorific}`
})

const previewAddress = computed(() => {
  const customer = selectedPreviewCustomer.value

  if (!customer) {
    return '〒421-3212　静岡県静岡市清水区蒲原小金389の2'
  }

  return customer.address || '住所未登録'
})

const previewClass = computed(() => {
  return form.envelopeType === 'KAKU2'
    ? 'envelope-preview kaku2-preview'
    : 'envelope-preview naga3-preview'
})

watch(
  () => props.modelValue,
  value => {
    if (!value) return
    form.customerIds = []
    form.envelopeType = 'NAGA3'
    form.stamp = '請求書在中'
    form.honorific = '御中'
    form.fontFamily = 'Yu Gothic'
    form.fontSize = 16
  },
)

function handleClose() {
  dialogModel.value = false
}

function handlePrint() {
  emit('print', {
    customerIds: [...form.customerIds],
    envelopeType: form.envelopeType,
    stamp: form.stamp,
    honorific: form.honorific,
    fontFamily: form.fontFamily,
    fontSize: form.fontSize,
  })

  dialogModel.value = false
}
</script>

<template>
  <v-dialog v-model="dialogModel" max-width="900">
    <v-card>
      <v-card-title>封筒宛名印刷</v-card-title>

      <v-card-text>
        <div class="d-flex flex-column ga-4">
          <v-select
            v-model="form.customerIds"
            :items="customers"
            item-title="name"
            item-value="id"
            label="印刷する企業"
            multiple
            chips
            closable-chips
            density="compact"
            hide-details="auto"
          />

          <div class="d-flex ga-3">
            <v-select
              v-model="form.envelopeType"
              :items="envelopeTypeOptions"
              item-title="title"
              item-value="value"
              label="封筒タイプ"
              density="compact"
              hide-details="auto"
            />

            <v-select
              v-model="form.stamp"
              :items="stampOptions"
              label="封筒スタンプ"
              density="compact"
              hide-details="auto"
            />

            <v-select
              v-model="form.honorific"
              :items="honorificOptions"
              label="敬称"
              density="compact"
              hide-details="auto"
            />
          </div>

          <div class="d-flex ga-3">
            <v-select
              v-model="form.fontFamily"
              :items="fontOptions"
              label="フォント"
              density="compact"
              hide-details="auto"
            />

            <v-text-field
              v-model.number="form.fontSize"
              label="文字サイズ"
              type="number"
              density="compact"
              hide-details="auto"
            />
          </div>

          <v-alert
            v-if="form.customerIds.length === 0"
            type="info"
            variant="tonal"
          >
            印刷する企業を1件以上選択してください。プレビューはサンプル表示です。
          </v-alert>

          <v-divider />

          <div>
            <div class="text-caption text-medium-emphasis mb-2">
              印刷イメージ
            </div>

            <div
              :class="previewClass"
              :style="{
                fontFamily: form.fontFamily,
                fontSize: `${form.fontSize}px`,
              }"
            >
              <div class="preview-address">
                {{ previewAddress }}
              </div>

              <div class="preview-name">
                {{ previewName }}
              </div>

              <div class="preview-line">
                ==================================================
              </div>

              <div class="preview-stamp">
                {{ form.stamp }}
              </div>
            </div>
          </div>
        </div>
      </v-card-text>

      <v-card-actions>
        <v-spacer />

        <v-btn variant="text" @click="handleClose">
          キャンセル
        </v-btn>

        <v-btn
          color="primary"
          :disabled="form.customerIds.length === 0"
          @click="handlePrint"
        >
          宛名印刷
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.envelope-preview {
  position: relative;
  width: 100%;
  border: 1px solid rgba(var(--v-theme-on-surface), 0.22);
  border-radius: 4px;
  background: #dfeff1;
  overflow: hidden;
  box-shadow: inset 0 0 18px rgba(255, 255, 255, 0.35);
}

/* 長3封筒：横長 */
.naga3-preview {
  aspect-ratio: 2.35 / 1;
}

/* 角2封筒：やや縦長寄り */
.kaku2-preview {
  aspect-ratio: 1.42 / 1;
}

/* 長3 */
.naga3-preview .preview-address {
  position: absolute;
  top: 22%;
  left: 18%;
  right: 8%;
  white-space: nowrap;
  letter-spacing: 0.06em;
  line-height: 1.4;
  font-size: 1.05em;
}

.naga3-preview .preview-name {
  position: absolute;
  top: 38%;
  left: 27%;
  right: 8%;
  white-space: nowrap;
  letter-spacing: 0.08em;
  line-height: 1.4;
  font-size: 1.35em;
}

.naga3-preview .preview-line {
  position: absolute;
  top: 47%;
  left: 18%;
  right: 8%;
  white-space: nowrap;
  overflow: hidden;
  letter-spacing: 0.02em;
  font-size: 1.05em;
  color: #222;
}

.naga3-preview .preview-stamp {
  position: absolute;
  left: 10.5%;
  top: 61%;
  border: 2px solid #1f3f6d;
  color: #1f3f6d;
  padding: 4px 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1.2;
  font-size: 1.15em;
  background: rgba(255, 255, 255, 0.08);
}

/* 角2 */
.kaku2-preview .preview-address {
  position: absolute;
  top: 18%;
  left: 20%;
  right: 8%;
  white-space: nowrap;
  letter-spacing: 0.06em;
  line-height: 1.4;
  font-size: 1.05em;
}

.kaku2-preview .preview-name {
  position: absolute;
  top: 31%;
  left: 28%;
  right: 8%;
  white-space: nowrap;
  letter-spacing: 0.08em;
  line-height: 1.4;
  font-size: 1.35em;
}

.kaku2-preview .preview-line {
  position: absolute;
  top: 38%;
  left: 20%;
  right: 8%;
  white-space: nowrap;
  overflow: hidden;
  letter-spacing: 0.02em;
  font-size: 1.05em;
  color: #222;
}

.kaku2-preview .preview-stamp {
  position: absolute;
  left: 14%;
  top: 52%;
  border: 2px solid #1f3f6d;
  color: #1f3f6d;
  padding: 4px 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1.2;
  font-size: 1.15em;
  background: rgba(255, 255, 255, 0.08);
}
</style>