<script setup lang="ts">
import { computed } from 'vue'
import type { Permission } from '@/features/users/types/types'
import { usePermissionGroups } from '@/features/users/composables/usePermissionGroups'

const props = defineProps<{
  modelValue: number[]
  items: Permission[] // ← 必須にする
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: number[]): void
}>()

// サーバーから来たデータをそのまま使う
const allPermissions = computed(() => props.items)

// グルーピング
const { groups } = usePermissionGroups(allPermissions)

// v-model
const model = computed({
  get: () => props.modelValue,
  set: (v: number[]) => emit('update:modelValue', v),
})

// 判定
const isSelected = (perm: Permission) =>
  model.value.includes(perm.id)

// トグル
const toggle = (perm: Permission) => {
  if (isSelected(perm)) {
    model.value = model.value.filter(id => id !== perm.id)
  } else {
    model.value = [...model.value, perm.id]
  }
}

// 選択済み
const selectedPermissions = computed(() =>
  allPermissions.value.filter(perm => model.value.includes(perm.id))
)
</script>

<template>
  <v-row>
    <!-- 左 -->
    <v-col cols="7">
      <v-card variant="outlined" class="pa-3" style="max-height: 400px; overflow-y: auto">
        <div
          v-for="group in groups"
          :key="group.label"
          style="margin-bottom: 16px"
        >
          <div class="text-subtitle-2 font-weight-bold mb-2">
            {{ group.label }}
          </div>

          <div style="display: flex; flex-wrap: wrap; gap: 6px">
            <v-chip
              v-for="perm in group.permissions"
              :key="perm.id"
              :color="isSelected(perm) ? 'primary' : ''"
              variant="outlined"
              size="small"
              style="cursor: pointer"
              @click="toggle(perm)"
            >
              {{ perm.label }}
            </v-chip>
          </div>
        </div>
      </v-card>
    </v-col>

    <!-- 右 -->
    <v-col cols="5">
      <v-card variant="outlined" class="pa-3" style="min-height: 400px">
        <div class="text-subtitle-2 font-weight-bold mb-2">
          選択中の権限
        </div>

        <div style="display: flex; flex-wrap: wrap; gap: 6px">
          <v-chip
            v-for="perm in selectedPermissions"
            :key="perm.id"
            color="primary"
            size="small"
            closable
            @click:close="toggle(perm)"
          >
            {{ perm.label }}
          </v-chip>
        </div>
      </v-card>
    </v-col>
  </v-row>
</template>