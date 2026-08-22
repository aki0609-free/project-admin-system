<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormLayout from '@/shared/components/form/base/FormLayout.vue'
import GridBasedForm from '@/shared/components/form/grid_based_form/GridBasedForm.vue'
import AppDialog from '@/shared/ui/dialog/AppDialog.vue'
import type { ToolbarItem } from '@/shared/ui/toolbar/types'

import { useCompanyProfileQuery } from '../api/useCompanyProfileQuery'
import { useSaveCompanyProfileMutation } from '../api/useSaveCompanyProfileMutation'

import { useCompanyProfileFormFields } from '../composables/useCompanyProfileFormFields'

import type { CompanyProfileForm } from '../types/companyProfileTypes'

import {
  createEmptyCompanyProfileForm,
  toCompanyProfileForm,
  toCompanyProfileSaveRequest,
} from '../utils/companyProfileFactory'
import { useAuth } from '@/shared/auth/composables/useAuth'
import { Role } from '@/shared/auth/types/types'
import { companyProfileSchema } from '../validation/companyProfileSchema'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const { hasRole } = useAuth()

const canManageCompanyProfile = computed(() => hasRole(Role.SYS_ADMIN))

const dialogModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const activeTab = ref('basic')
const editMode = ref(false)
const operationMessage = ref('')
const operationMessageType = ref<'success' | 'error'>('success')
let operationMessageTimer: ReturnType<typeof window.setTimeout> | undefined

const form = reactive<CompanyProfileForm>(createEmptyCompanyProfileForm())

const companyProfileQuery = useCompanyProfileQuery()
const { companyProfile, isFetching, refetch } = companyProfileQuery

const saveMutation = useSaveCompanyProfileMutation()

const { tabs, basicFields, invoiceFields, certificationFields } = useCompanyProfileFormFields()

const schema = companyProfileSchema

const loading = computed(() => isFetching.value || saveMutation.isPending.value)

const displayCompanyName = computed(() => form.companyName || '会社情報未登録')
const loadError = computed(() => companyProfileQuery.isError.value)

const displayAddress = computed(() => {
  const address = [form.prefecture, form.city, form.addressLine1, form.addressLine2]
    .filter(Boolean)
    .join('')

  if (!form.postalCode) {
    return address
  }

  return address ? `〒${form.postalCode}\n${address}` : `〒${form.postalCode}`
})

const businessContents = computed(() =>
  form.businessContentsText
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean),
)

const certificationItems = computed(() =>
  form.certificationInformationText
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean),
)

function applyProfile() {
  if (!companyProfile.value) {
    Object.assign(form, createEmptyCompanyProfileForm())
    return
  }

  Object.assign(form, toCompanyProfileForm(companyProfile.value))
}

watch(
  () => props.modelValue,
  async (opened) => {
    if (!opened) {
      editMode.value = false
      return
    }

    activeTab.value = 'basic'

    await refetch()
    applyProfile()
  },
)

watch(
  companyProfile,
  () => {
    if (!dialogModel.value) return
    if (editMode.value) return

    applyProfile()
  },
  {
    immediate: true,
  },
)

function startEdit() {
  if (!canManageCompanyProfile.value) {
    return
  }

  editMode.value = true
  operationMessage.value = ''
}

function cancelEdit() {
  applyProfile()
  editMode.value = false
}

async function save() {
  if (!canManageCompanyProfile.value) {
    showMessage('会社情報を編集する権限がありません。', 'error')
    return
  }

  const parsed = schema.safeParse(form)

  if (!parsed.success) {
    showMessage('入力内容を確認してください。', 'error')
    return
  }

  try {
    await saveMutation.mutateAsync(toCompanyProfileSaveRequest(form))

    await refetch()
    applyProfile()

    editMode.value = false
    showMessage('会社情報を保存しました。', 'success')
  } catch (error) {
    showMessage(toErrorMessage(error, '会社情報の保存に失敗しました。'), 'error')
  }
}

function close() {
  if (loading.value) return
  dialogModel.value = false
}

function showMessage(message: string, type: 'success' | 'error') {
  clearOperationMessageTimer()
  operationMessage.value = message
  operationMessageType.value = type
  operationMessageTimer = window.setTimeout(
    () => {
      operationMessage.value = ''
      operationMessageTimer = undefined
    },
    type === 'success' ? 4000 : 8000,
  )
}

function clearOperationMessageTimer() {
  if (operationMessageTimer === undefined) return
  window.clearTimeout(operationMessageTimer)
  operationMessageTimer = undefined
}

function dismissOperationMessage() {
  clearOperationMessageTimer()
  operationMessage.value = ''
}

onBeforeUnmount(clearOperationMessageTimer)

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'object' && error !== null) {
    const candidate = error as Record<string, unknown>
    if (typeof candidate.message === 'string') return candidate.message
  }
  return fallback
}

const rightFooterItems = computed<ToolbarItem[]>(() => {
  if (editMode.value) {
    return [
      {
        type: 'button',
        label: 'キャンセル',
        intent: 'utility',
        disabled: loading.value,
        onClick: cancelEdit,
      },
      {
        type: 'button',
        label: '保存',
        intent: 'primary',
        loading: saveMutation.isPending.value,
        disabled: loading.value,
        onClick: () => void save(),
      },
    ]
  }

  return [
    {
      type: 'button',
      label: '閉じる',
      intent: 'primary',
      onClick: close,
    },
  ]
})
</script>

<template>
  <AppDialog
    v-model="dialogModel"
    title="会社情報"
    size="xl"
    :max-width="1200"
    :right-footer-items="rightFooterItems"
    body-class="company-dialog-body"
  >
    <template #title>
      <div class="title-content">
        <v-icon color="primary">mdi-domain</v-icon>
        <div>
          <h2 class="title-main">会社情報</h2>
          <div class="title-sub">{{ displayCompanyName }}</div>
        </div>
      </div>
    </template>

    <template #header-actions>
      <div class="header-actions">
        <v-btn
          v-if="!editMode && canManageCompanyProfile"
          color="primary"
          variant="tonal"
          prepend-icon="mdi-pencil"
          @click="startEdit"
        >
          編集
        </v-btn>
      </div>
    </template>

    <v-progress-linear v-if="loading" indeterminate class="mb-3" />

    <v-alert
      v-if="loadError"
      type="error"
      variant="tonal"
      class="mb-3"
      title="会社情報を取得できませんでした"
    >
      <template #append>
        <v-btn variant="text" @click="refetch">再試行</v-btn>
      </template>
    </v-alert>

    <v-alert
      v-if="operationMessage"
      :type="operationMessageType"
      variant="tonal"
      closable
      class="mb-3"
      @click:close="dismissOperationMessage"
    >
      {{ operationMessage }}
    </v-alert>

    <TabLayout v-model="activeTab" :tabs="tabs">
      <template #default="{ active }">
        <template v-if="editMode">
          <FormLayout v-if="active === 'basic'" v-model="form" :schema="schema">
            <GridBasedForm v-model="form" :fields="basicFields" />
          </FormLayout>

          <FormLayout v-else-if="active === 'invoice'" v-model="form" :schema="schema">
            <GridBasedForm v-model="form" :fields="invoiceFields" />
          </FormLayout>

          <FormLayout v-else-if="active === 'certification'" v-model="form" :schema="schema">
            <GridBasedForm v-model="form" :fields="certificationFields" />
          </FormLayout>
        </template>

        <template v-else>
          <div v-if="active === 'basic'" class="profile-grid">
            <v-card variant="outlined" class="profile-card">
              <v-card-title> 会社概要 </v-card-title>

              <v-divider />

              <v-card-text>
                <div class="info-row">
                  <div class="info-label">会社コード</div>

                  <div class="info-value">
                    {{ form.companyCode || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">商号</div>

                  <div class="info-value">
                    {{ form.companyName || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">商号カナ・略称</div>

                  <div class="info-value">
                    {{ [form.companyNameKana, form.shortName].filter(Boolean).join(' / ') || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">代表者</div>

                  <div class="info-value">
                    {{
                      [form.representativeTitle, form.representativeName]
                        .filter(Boolean)
                        .join(' ') || '-'
                    }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">所在地</div>

                  <div class="info-value address">
                    {{ displayAddress || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">電話</div>

                  <div class="info-value">
                    {{ form.phone || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">FAX</div>

                  <div class="info-value">
                    {{ form.fax || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">メール</div>

                  <div class="info-value">
                    {{ form.email || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">Webサイト</div>

                  <div class="info-value">
                    <a
                      v-if="form.websiteUrl"
                      :href="form.websiteUrl"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {{ form.websiteUrl }}
                    </a>
                    <template v-else>-</template>
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">対応エリア</div>

                  <div class="info-value">
                    {{ form.serviceArea || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">資本金</div>

                  <div class="info-value">
                    {{ form.capitalAmount ? `${form.capitalAmount}円` : '-' }}
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </div>

          <div v-else-if="active === 'invoice'" class="profile-grid">
            <v-card variant="outlined" class="profile-card">
              <v-card-title> 請求書設定 </v-card-title>

              <v-divider />

              <v-card-text>
                <div class="info-row">
                  <div class="info-label">登録番号</div>

                  <div class="info-value">
                    {{ form.qualifiedInvoiceIssuerNumber || '-' }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">振込先</div>

                  <div class="info-value address">
                    {{
                      [
                        [form.invoiceBankName, form.invoiceBankBranchName]
                          .filter(Boolean)
                          .join(' '),

                        [form.invoiceBankAccountType, form.invoiceBankAccountNumber]
                          .filter(Boolean)
                          .join(' '),

                        form.invoiceBankAccountHolder,
                      ]
                        .filter(Boolean)
                        .join('\n') || '-'
                    }}
                  </div>
                </div>

                <div class="info-row">
                  <div class="info-label">備考</div>

                  <div class="info-value address">
                    {{ form.invoiceNote || '-' }}
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </div>

          <div v-else-if="active === 'certification'" class="profile-grid">
            <v-card variant="outlined" class="profile-card">
              <v-card-title> 事業内容 </v-card-title>

              <v-divider />

              <v-card-text>
                <div class="info-row">
                  <div class="info-label">代表許可番号</div>

                  <div class="info-value">
                    {{ form.permitNumber || '-' }}
                  </div>
                </div>

                <div class="chip-wrap">
                  <v-chip
                    v-for="item in businessContents"
                    :key="item"
                    color="primary"
                    variant="outlined"
                  >
                    {{ item }}
                  </v-chip>

                  <span v-if="businessContents.length === 0" class="empty-text">
                    登録されていません。
                  </span>
                </div>
              </v-card-text>
            </v-card>

            <v-card variant="outlined" class="profile-card">
              <v-card-title> 許認可・資格情報 </v-card-title>

              <v-divider />

              <v-card-text>
                <div class="chip-wrap">
                  <v-chip
                    v-for="item in certificationItems"
                    :key="item"
                    color="secondary"
                    variant="outlined"
                  >
                    {{ item }}
                  </v-chip>

                  <span v-if="certificationItems.length === 0" class="empty-text">
                    登録されていません。
                  </span>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </template>
      </template>
    </TabLayout>
  </AppDialog>
</template>

<style scoped>
.title-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-main {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.title-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.profile-grid {
  display: grid;
  gap: 16px;
  padding: 8px 0;
}

.profile-card {
  background: white;
  border-radius: 14px;
}

.info-row {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #e2e8f0;
}

.info-row:last-child {
  border-bottom: 0;
}

.info-label {
  font-weight: 700;
  color: #475569;
}

.info-value {
  color: #0f172a;
}

.address {
  white-space: pre-line;
  line-height: 1.8;
}

.chip-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: #94a3b8;
}
</style>
