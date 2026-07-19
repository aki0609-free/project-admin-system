<script setup lang="ts">
withDefaults(
  defineProps<{
    leftWidth?: string
    rightWidth?: string
    gap?: string
  }>(),
  {
    leftWidth: '1fr',
    rightWidth: '420px',
    gap: '16px',
  },
)
</script>

<template>
  <div
    class="split-editor-tab"
    :style="{
      gridTemplateColumns: `${leftWidth} ${rightWidth}`,
      gap,
    }"
  >
    <div class="split-pane left-pane">
      <slot name="left" />
    </div>

    <div class="split-pane right-pane">
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped>
.split-editor-tab {
  display: grid;
  align-items: start;
  min-width: 0;
  padding-top: 16px;
}

.split-pane {
  min-width: 0;
}

.right-pane {
  position: sticky;
  top: 0;
}
</style>