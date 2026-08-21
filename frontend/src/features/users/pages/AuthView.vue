<script setup lang="ts">
import { computed } from 'vue'
import { z } from 'zod'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'
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

const leftToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '新規追加',
    intent: 'primary',
    disabled: loading.value,
    onClick: openCreate,
  },
])

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

const leftFooterItems = computed<ToolbarItem[]>(() =>
  form.value.id
    ? [
        {
          type: 'button',
          label: '削除',
          intent: 'danger',
          loading: deleting.value,
          onClick: removeCurrentRole,
        },
      ]
    : [],
)

const rightFooterItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: 'キャンセル',
    intent: 'secondary',
    onClick: closeDialog,
  },
  {
    type: 'button',
    label: '保存',
    intent: 'primary',
    loading: saving.value,
    onClick: save,
  },
])

const filterRules = computed(() =>
  createSimpleTableFilterRules<RoleDetail>(columns.value)
)

const formatPermission = (name: string) => name
</script>

<template>
  <ListDetailPageLayout
    title="権限管理"
    description="ロールごとに利用可能な画面と操作権限を管理します。"
    :left-toolbar-items="leftToolbarItems"
  >
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
        <template #[`item.permissions`]="{ item }: { item: RoleDetail }">
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

    <template #dialogs>
      <AppDialog
        v-model="dialog"
        :title="form.id ? 'ロール編集' : 'ロール新規作成'"
        size="md"
        body-layout="stack"
        :left-footer-items="leftFooterItems"
        :right-footer-items="rightFooterItems"
      >
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
      </AppDialog>
    </template>
  </ListDetailPageLayout>
</template>
