<script setup lang="ts">
import { computed } from 'vue'
import { z } from 'zod'

import CardLayout from '@/shared/components/layout/card_layout/CardLayout.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'

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

</script>

<template>
  <CardLayout title="ユーザ一覧">
    <div style="margin-bottom: 12px; display: flex; gap: 8px">
      <v-btn color="primary" @click="openCreate"> 新規追加 </v-btn>
    </div>

    <SimpleTable
      tableKey="users"
      itemKey="id"
      :items="users"
      :columns="columns"
      :filterRules="filterRules"
      :enableRowClick="true"
      :loading="loading"
      @row-click="openEdit"
    >
      <template #item.roles="{ item }">
        <div style="display: flex; gap: 4px; flex-wrap: wrap">
          <v-chip v-for="role in item.roles" :key="role" size="small" variant="outlined">
            {{ roleLabelMap[role] ?? role }}
          </v-chip>
        </div>
      </template>
    </SimpleTable>

    <v-dialog v-model="dialog" width="720">
      <v-card>
        <v-card-title>
          {{ isEdit ? 'ユーザー編集' : 'ユーザー新規作成' }}
        </v-card-title>

        <v-card-text>
          <FormLayout v-model="form" :schema="schema">
            <GridBasedForm v-model="form" :fields="fields" />
          </FormLayout>
          <RolePermissionPanel :roles="selectedRoleDetails" />
        </v-card-text>
        <v-card-actions>
          <v-btn
            v-if="isEdit"
            color="error"
            variant="text"
            :loading="deleting"
            @click="removeCurrentUser"
          >
            削除
          </v-btn>
          <v-spacer />
          <v-btn variant="text" @click="closeDialog"> キャンセル </v-btn>

          <v-btn color="primary" :loading="saving" @click="save"> 保存 </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </CardLayout>
</template>
