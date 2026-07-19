<script setup lang="ts">
defineProps<{
  previewSrc?: string | null
}>()

const emit = defineEmits<{
  (e: 'file-change', event: Event): void
}>()
</script>

<template>
  <div class="signature-image-field">
    <div class="upload-block">
      <label class="field-label">署名画像</label>

      <v-file-input
        accept="image/*"
        label="画像を選択"
        variant="outlined"
        density="comfortable"
        hide-details
        @change="(event: Event) => emit('file-change', event)"
      />
    </div>

    <div
      v-if="previewSrc"
      class="preview-block"
    >
      <label class="field-label">プレビュー</label>

      <div class="preview-box">
        <img
          :src="previewSrc"
          alt="signature preview"
          class="preview-image"
        >
      </div>
    </div>
  </div>
</template>

<style scoped>
.signature-image-field {
  display: grid;
  gap: 16px;
  margin-top: 16px;
}

.upload-block,
.preview-block {
  display: grid;
  gap: 8px;
}

.field-label {
  font-weight: 600;
}

.preview-box {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
}

.preview-image {
  max-width: 100%;
  max-height: 220px;
  display: block;
}
</style>