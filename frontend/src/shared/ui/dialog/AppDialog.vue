<script setup lang="ts">
import { computed } from 'vue'
import AppToolbar from '@/shared/ui/toolbar/AppToolbar.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
import type { AppDialogSize } from './types'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    size?: AppDialogSize
    maxWidth?: string | number
    scrollable?: boolean
    persistent?: boolean
    closable?: boolean
    bodyClass?: string
    bodyLayout?: 'plain' | 'stack'
    leftFooterItems?: ToolbarItem[]
    rightFooterItems?: ToolbarItem[]
  }>(),
  {
    size: 'lg',
    maxWidth: undefined,
    scrollable: true,
    persistent: true,
    closable: false,
    bodyClass: '',
    bodyLayout: 'plain',
    leftFooterItems: () => [],
    rightFooterItems: () => [],
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const sizeWidths: Record<Exclude<AppDialogSize, 'fullscreen'>, number> = {
  sm: 480,
  md: 720,
  lg: 1080,
  xl: 1280,
}

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const fullscreen = computed(() => props.size === 'fullscreen')
const resolvedMaxWidth = computed(() => {
  if (fullscreen.value) return undefined
  return props.maxWidth ?? sizeWidths[props.size as Exclude<AppDialogSize, 'fullscreen'>]
})
const hasFooter = computed(
  () =>
    props.leftFooterItems.some((item) => item.visible !== false) ||
    props.rightFooterItems.some((item) => item.visible !== false),
)

const close = () => {
  visible.value = false
}
</script>

<template>
  <v-dialog
    v-model="visible"
    :width="fullscreen ? undefined : 'calc(100vw - 32px)'"
    :max-width="resolvedMaxWidth"
    :fullscreen="fullscreen"
    :scrollable="scrollable"
    :persistent="persistent"
  >
    <v-card class="app-dialog-card">
      <div class="app-dialog-header">
        <div class="app-dialog-title-area">
          <slot name="title">
            <h2 class="app-dialog-title">{{ title }}</h2>
          </slot>
          <slot name="subtitle" />
        </div>

        <div class="app-dialog-header-actions">
          <slot name="header-actions" />
          <v-btn
            v-if="closable"
            icon="mdi-close"
            variant="text"
            aria-label="閉じる"
            @click="close"
          />
        </div>
      </div>

      <v-divider />

      <v-card-text class="app-dialog-body" :class="[bodyClass, `layout-${bodyLayout}`]">
        <slot />
      </v-card-text>

      <template v-if="hasFooter || $slots.footer">
        <v-divider />
        <div class="app-dialog-footer">
          <slot name="footer">
            <AppToolbar
              :left-items="leftFooterItems"
              :right-items="rightFooterItems"
              surface="dialog"
            />
          </slot>
        </div>
      </template>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.app-dialog-card {
  width: 100%;
  max-height: calc(100dvh - 32px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-dialog-header {
  min-height: 60px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.app-dialog-title-area {
  min-width: 0;
}

.app-dialog-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
  color: #0f172a;
}

.app-dialog-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.app-dialog-body {
  display: block;
  padding: 16px 20px;
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1 1 auto;
  min-height: 0;
  max-height: calc(100dvh - 154px);
}

.app-dialog-body.layout-stack {
  display: grid;
  align-content: start;
  grid-auto-rows: max-content;
  gap: 16px;
}

.app-dialog-footer {
  padding: 4px 12px;
  flex-shrink: 0;
  background: #ffffff;
}

.v-dialog--fullscreen .app-dialog-card,
.v-dialog--fullscreen .app-dialog-body {
  max-height: none;
}

@media (max-width: 720px) {
  .app-dialog-card {
    max-height: calc(100dvh - 16px);
  }

  .app-dialog-header {
    min-height: 52px;
    padding: 10px 12px;
    gap: 8px;
  }

  .app-dialog-title {
    font-size: 18px;
  }

  .app-dialog-body {
    padding: 12px;
    max-height: calc(100dvh - 132px);
  }

  .app-dialog-footer {
    padding-inline: 8px;
  }
}
</style>
