<script setup lang="ts">
import { computed } from 'vue'
import { PropType } from 'vue'

type Direction = 'horizontal' | 'vertical'

const props = defineProps({
  direction: {
    type: String as PropType<Direction>,
    default: 'horizontal',
  },

  /* 横分割用（%） */
  ratio: {
    type: Number,
    default: 30,
  },

  /* 縦分割用（px固定） */
  fixed: {
    type: Number,
    default: 200,
  },

  divider: {
    type: Boolean,
    default: true,
  },
})

const isHorizontal = computed(() => props.direction === 'horizontal')

const firstStyle = computed(() => {
  if (isHorizontal.value) {
    return {
      width: props.ratio + '%',
      minWidth: '180px',
    }
  }
  return {
    height: props.fixed + 'px',
  }
})
</script>

<template>
  <div
    class="split-layout"
    :class="direction"
  >
    <div
      class="split-pane first"
      :style="firstStyle"
    >
      <slot name="first" />
    </div>

    <div
      v-if="divider"
      class="split-divider"
    />

    <div class="split-pane second">
      <slot name="second" />
    </div>
  </div>
</template>

<style scoped>
.split-layout {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* 横分割 */
.split-layout.horizontal {
  flex-direction: row;
}

/* 縦分割 */
.split-layout.vertical {
  flex-direction: column;
}

/* pane共通 */
.split-pane {
  overflow: auto;
}

/* first固定 */
.split-pane.first {
  flex: 0 0 auto;
}

/* secondは残り全部 */
.split-pane.second {
  flex: 1 1 auto;
}

/* divider */
.split-divider {
  background: rgba(0, 0, 0, 0.06);
  flex: 0 0 auto;
}

/* 横 */
.horizontal .split-divider {
  width: 1px;
  box-shadow: 2px 0 4px rgba(0, 0, 0, 0.04);
}

/* 縦 */
.vertical .split-divider {
  height: 1px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}
</style>