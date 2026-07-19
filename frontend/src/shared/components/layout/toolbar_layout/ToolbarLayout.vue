<script setup lang="ts">
defineProps({
  fluid: {
    type: Boolean,
    default: true,
  },
})
</script>

<template>
  <div class="toolbar-layout">
    <!-- Top Toolbar -->
    <div class="toolbar-area">
      <slot name="toolbar" />
    </div>

    <!-- Content Area -->
    <div
      class="content-area"
      :class="{ fluid }"
    >
      <slot />
    </div>
  </div>
</template>

<style scoped>
.toolbar-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* 上部固定 */
.toolbar-area {
  flex: 0 0 auto;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: #ffffff;
  z-index: 1;
}

/* 下は残り全部 */
.content-area {
  flex: 1 1 auto;
  overflow: auto;
  padding: 16px;
  background: #f5f7fa;
}

/* 余白なしモード */
.content-area:not(.fluid) {
  padding: 0;
}
</style>