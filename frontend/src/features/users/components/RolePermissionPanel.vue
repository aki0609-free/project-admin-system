<script setup lang="ts">
import type { RoleDetail } from '@/features/users/types/types'

defineProps<{
  roles: RoleDetail[]
}>()

const formatPermission = (perm: string) => {
  return perm
    .split(':')
    .map(v => v.toUpperCase())
    .join(' ')
}
</script>

<template>
  <div v-if="roles.length">
    <div
      style="
        font-size: 14px;
        font-weight: 600;
        margin-bottom: 12px;
      "
    >
      ロール権限
    </div>

    <div
      v-for="role in roles"
      :key="role.id"
      style="
        margin-bottom: 16px;
        padding: 12px;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
      "
    >
      <div
        style="
          font-size: 13px;
          font-weight: 600;
          margin-bottom: 8px;
        "
      >
        {{ role.name }}
      </div>

      <div style="display: flex; gap: 6px; flex-wrap: wrap">
        <v-chip
          v-for="perm in role.permissions"
          :key="perm.id"
          color="green"
          size="small"
          variant="outlined"
        >
          {{ formatPermission(perm.label) }}
        </v-chip>
      </div>
    </div>
  </div>
</template>