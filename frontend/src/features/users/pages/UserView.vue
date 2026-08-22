<script setup lang="ts">
import { computed, ref } from 'vue'
import { z } from 'zod'

import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

import type { RoleDetail, UserListItem } from '@/features/users/types/types'

import { useUsersQuery } from '@/features/users/api/useUsersQuery'
import { useRolesQuery } from '@/features/users/api/useRolesQuery'
import { useCreateUserMutation } from '@/features/users/api/useCreateUserMutation'
import { useUpdateUserMutation } from '@/features/users/api/useUpdateUserMutation'
import { useDeleteUserMutation } from '@/features/users/api/useDeleteUserMutation'

import { useRoleOptions } from '@/features/users/composables/useRoleOptions'
import { useUserColumns } from '@/features/users/composables/useUserColumns'
import { useUserForm } from '@/features/users/composables/useUserForm'
import { useUserFormFields } from '@/features/users/composables/useUserFormFields'

import { toUserCreatePayload, toUserUpdatePayload } from '@/features/users/utils/userFormMapper'
import RolePermissionPanel from '../components/RolePermissionPanel.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'

// ----------------------
// Query
// ----------------------
const usersQuery = useUsersQuery()
const rolesQuery = useRolesQuery()

const users = computed(() => usersQuery.data.value ?? [])
const roles = computed<RoleDetail[]>(() => rolesQuery.data.value ?? [])
const selectedRoleDetails = computed(() =>
  roles.value.filter(role => form.value.roles.includes(role.name))
)

// ----------------------
// Mutations
// ----------------------
const createUserMutation = useCreateUserMutation()
const updateUserMutation = useUpdateUserMutation()
const deleteUserMutation = useDeleteUserMutation()

// ----------------------
// Form state
// ----------------------
const { dialog, isEdit, form, openCreate, openEdit, closeDialog } = useUserForm()
const formLayoutRef = ref<{ validateAll: () => boolean } | null>(null)

// ----------------------
// Derived UI state
// ----------------------
const { roleOptions, roleLabelMap } = useRoleOptions(roles)
const { columns } = useUserColumns(roleOptions)
const { fields } = useUserFormFields(roleOptions, isEdit)

const filterRules = computed(() =>
  createSimpleTableFilterRules<UserListItem>(columns.value)
)

const loading = computed(() => usersQuery.isLoading.value || rolesQuery.isLoading.value)

const saving = computed(
  () => createUserMutation.isPending.value || updateUserMutation.isPending.value,
)

const deleting = computed(() => deleteUserMutation.isPending.value)

const leftToolbarItems = computed<ToolbarItem[]>(() => [
  {
    type: 'button',
    label: '新規追加',
    intent: 'primary',
    disabled: loading.value,
    onClick: openCreate,
  },
])

// ----------------------
// Validation schema
// ----------------------
const schema = computed(() =>
  z.object({
    id: z.number().nullable(),
    username: z.string().min(1, 'ユーザー名は必須です'),
    password: isEdit.value ? z.string() : z.string().min(1, 'パスワードは必須です'),
    enabled: z.boolean(),
    roles: z.array(z.string()).min(1, 'ロールを1つ以上選択してください'),
  }),
)

// ----------------------
// Actions
// ----------------------
const save = async () => {
  if (!formLayoutRef.value?.validateAll()) return

  if (isEdit.value) {
    await updateUserMutation.mutateAsync(toUserUpdatePayload(form.value))
  } else {
    await createUserMutation.mutateAsync(toUserCreatePayload(form.value))
  }

  closeDialog()
}

const removeCurrentUser = async () => {
  if (form.value.id == null) return

  const ok = window.confirm(`「${form.value.username}」を削除しますか？`)
  if (!ok) return

  await deleteUserMutation.mutateAsync(form.value.id)
  closeDialog()
}

const leftFooterItems = computed<ToolbarItem[]>(() =>
  isEdit.value
    ? [
        {
          type: 'button',
          label: '削除',
          intent: 'danger',
          loading: deleting.value,
          onClick: removeCurrentUser,
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

</script>

<template>
  <ListDetailPageLayout
    title="ユーザー管理"
    description="ログインユーザー、利用状態、割り当てロールを管理します。"
    :left-toolbar-items="leftToolbarItems"
  >
    <SimpleTable
      table-key="users"
      item-key="id"
      :items="users"
      :columns="columns"
      :filter-rules="filterRules"
      :enable-row-click="true"
      :loading="loading"
      @row-click="openEdit"
    >
      <template #[`item.roles`]="{ item }">
        <div style="display: flex; gap: 4px; flex-wrap: wrap">
          <v-chip v-for="role in item.roles" :key="role" size="small" variant="outlined">
            {{ roleLabelMap[role] ?? role }}
          </v-chip>
        </div>
      </template>
    </SimpleTable>

    <template #dialogs>
      <AppDialog
        v-model="dialog"
        :title="isEdit ? 'ユーザー編集' : 'ユーザー新規作成'"
        size="md"
        body-layout="stack"
        :left-footer-items="leftFooterItems"
        :right-footer-items="rightFooterItems"
      >
          <FormLayout ref="formLayoutRef" v-model="form" :schema="schema">
            <GridBasedForm v-model="form" :fields="fields" />
          </FormLayout>
          <RolePermissionPanel :roles="selectedRoleDetails" />
      </AppDialog>
    </template>
  </ListDetailPageLayout>
</template>
