<script setup lang="ts">
import { computed } from 'vue'
import { z } from 'zod'
import CardLayout from '@/shared/components/layout/card_layout/CardLayout.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import PermissionSelector from '@/features/users/components/PermissionSelector.vue'

import type { RoleDetail } from '@/features/users/types/types'
import { useRolesQuery } from '@/features/users/api/useRolesQuery'
import { usePermissionsQuery } from '@/features/users/api/usePermissionsQuery'
import { useCreateRoleMutation } from '@/features/users/api/useCreateRoleMutation'
import { useUpdateRoleMutation } from '@/features/users/api/useUpdateRoleMutation'
import { useDeleteRoleMutation } from '@/features/users/api/useDeleteRoleMutation'
import { useRoleForm } from '@/features/users/composables/useRoleForm'
import { roleFormFields } from '@/features/users/composables/useRoleFormFields'
import { useRoleColumns } from '@/features/users/composables/useRoleColumns'
import {
  toRoleCreatePayload,
  toRoleUpdatePayload,
} from '@/features/users/utils/roleFormMapper'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'

const rolesQuery = useRolesQuery()
const permissionsQuery = usePermissionsQuery()

const createRoleMutation = useCreateRoleMutation()
const updateRoleMutation = useUpdateRoleMutation()
const deleteRoleMutation = useDeleteRoleMutation()

const { dialog, form, openCreate, openEdit, closeDialog } = useRoleForm()

const roles = computed(() => rolesQuery.data.value ?? [])
const allPermissions = computed(() => permissionsQuery.permissions.value ?? [])

const permissionFilterOptions = computed(() =>
  allPermissions.value.map((permission) => ({
    title: permission.label,
    value: permission.name
  }))
)
const { columns } = useRoleColumns(permissionFilterOptions)

const loading = computed(
  () => rolesQuery.isLoading.value || permissionsQuery.isLoading.value,
)

const saving = computed(
  () =>
    createRoleMutation.isPending.value ||
    updateRoleMutation.isPending.value,
)

const deleting = computed(() => deleteRoleMutation.isPending.value)

const schema = z.object({
  id: z.number().nullable(),
  name: z.string().min(1, 'ロール名は必須です'),
  permissionIds: z.array(z.number()).min(1, '権限を1つ以上選択してください'),
})

const save = async () => {
  if (form.value.id == null) {
    await createRoleMutation.mutateAsync(
      toRoleCreatePayload(form.value),
    )
  } else {
    await updateRoleMutation.mutateAsync(
      toRoleUpdatePayload(form.value),
    )
  }

  closeDialog()
}

const removeCurrentRole = async () => {
  if (form.value.id == null) return

  const ok = window.confirm(`「${form.value.name}」を削除しますか？`)
  if (!ok) return

  await deleteRoleMutation.mutateAsync(form.value.id)
  closeDialog()
}

const filterRules = computed(() =>
  createSimpleTableFilterRules<RoleDetail>(columns.value)
)

const formatPermission = (name: string) => name
</script>

<template>
  <v-container>
    <CardLayout title="権限管理">
      <v-btn color="primary" @click="openCreate">
        新規追加
      </v-btn>

      <SimpleTable
        table-key="role-table"
        item-key="id"
        :items="roles"
        :columns="columns"
        :filter-rules="filterRules"
        :enable-row-click="true"
        :loading="loading"
        @row-click="openEdit"
      >
        <template #item.permissions="{ item }: { item: RoleDetail }">
          <div style="display: flex; gap: 4px; flex-wrap: wrap">
            <v-chip
              v-for="perm in item.permissions"
              :key="perm.id"
              size="small"
              variant="outlined"
            >
              {{ formatPermission(perm.label) }}
            </v-chip>
          </div>
        </template>
      </SimpleTable>
    </CardLayout>

    <v-dialog v-model="dialog" max-width="700">
      <v-card>
        <v-card-title>
          {{ form.id ? 'ロール編集' : 'ロール新規作成' }}
        </v-card-title>

        <v-card-text>
          <FormLayout
            v-model="form"
            :schema="schema"
          >
            <GridBasedForm
              v-model="form"
              :fields="roleFormFields"
            />
          </FormLayout>

          <div style="margin-top: 16px">
            <div class="text-subtitle-2 font-weight-bold mb-2">
              権限
            </div>

            <PermissionSelector
              v-model="form.permissionIds"
              :items="allPermissions"
            />
          </div>
        </v-card-text>

        <v-card-actions>
          <v-btn
            v-if="form.id"
            color="error"
            variant="text"
            :loading="deleting"
            @click="removeCurrentRole"
          >
            削除
          </v-btn>

          <v-spacer />

          <v-btn variant="text" @click="closeDialog">
            キャンセル
          </v-btn>

          <v-btn
            color="primary"
            :loading="saving"
            @click="save"
          >
            保存
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>