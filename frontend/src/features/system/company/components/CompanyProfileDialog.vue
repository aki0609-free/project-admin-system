<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { z } from 'zod'

import TabLayout from '@/shared/components/layout/tab_layout/TabLayout.vue'
import FormGridTab from '@/toolbox/tab/FormGridTab.vue'

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

const form = reactive<CompanyProfileForm>(createEmptyCompanyProfileForm())

const { companyProfile, isFetching, refetch } = useCompanyProfileQuery()

const saveMutation = useSaveCompanyProfileMutation()

const { tabs, basicFields, invoiceFields, certificationFields } = useCompanyProfileFormFields()

const schema = z
  .object({
    companyCode: z.string().trim().min(1, '会社コードは必須です'),

    companyName: z.string().trim().min(1, '会社名は必須です'),

    capitalAmount: z.number().min(0).nullable(),

    activeFlag: z.boolean(),
  })
  .passthrough()

const loading = computed(() => isFetching.value || saveMutation.isPending.value)

const displayCompanyName = computed(() => form.companyName || '会社情報未登録')

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
}

function cancelEdit() {
  applyProfile()
  editMode.value = false
}

async function save() {
  if (!canManageCompanyProfile.value) {
    window.alert('会社情報を編集する権限がありません。')
    return
  }

  const parsed = schema.safeParse(form)

  if (!parsed.success) {
    window.alert(parsed.error.issues.map((issue) => issue.message).join('\n'))
    return
  }

  await saveMutation.mutateAsync(toCompanyProfileSaveRequest(form))

  await refetch()
  applyProfile()

  editMode.value = false
}

function close() {
  dialogModel.value = false
}
</script>

<template>
  <v-dialog v-model="dialogModel" max-width="1200" scrollable>
    <v-card rounded="xl" class="company-dialog">
      <v-card-title class="company-dialog-title">
        <div class="title-content">
          <v-icon> mdi-domain </v-icon>

          <div>
            <div class="title-main">会社情報</div>

            <div class="title-sub">
              {{ displayCompanyName }}
            </div>
          </div>
        </div>

        <v-spacer />

        <v-btn
          v-if="!editMode && canManageCompanyProfile"
          color="white"
          variant="tonal"
          prepend-icon="mdi-pencil"
          @click="startEdit"
        >
          編集
        </v-btn>

        <v-btn icon="mdi-close" variant="text" color="white" @click="close" />
      </v-card-title>

      <v-progress-linear v-if="loading" indeterminate />

      <v-card-text class="dialog-content">
        <TabLayout v-model="activeTab" :tabs="tabs">
          <template #default="{ active }">
            <template v-if="editMode">
              <FormGridTab
                v-if="active === 'basic'"
                v-model="form"
                :schema="schema"
                :fields="basicFields"
              />

              <div v-else-if="active === 'invoice'" class="form-section">
                <FormGridTab v-model="form" :schema="schema" :fields="invoiceFields" />

                <v-textarea
                  v-model="form.invoiceNote"
                  label="請求書備考"
                  variant="outlined"
                  rows="5"
                  auto-grow
                  hide-details
                />
              </div>

              <div v-else-if="active === 'certification'" class="form-section">
                <FormGridTab v-model="form" :schema="schema" :fields="certificationFields" />

                <v-textarea
                  v-model="form.businessContentsText"
                  label="事業内容"
                  hint="1行につき1項目入力してください。"
                  persistent-hint
                  variant="outlined"
                  rows="8"
                  auto-grow
                />

                <v-textarea
                  v-model="form.certificationInformationText"
                  label="許認可・資格情報"
                  hint="1行につき1項目入力してください。"
                  persistent-hint
                  variant="outlined"
                  rows="8"
                  auto-grow
                />
              </div>
            </template>

            <template v-else>
              <div v-if="active === 'basic'" class="profile-grid">
                <v-card variant="outlined" class="profile-card">
                  <v-card-title> 会社概要 </v-card-title>

                  <v-divider />

                  <v-card-text>
                    <div class="info-row">
                      <div class="info-label">商号</div>

                      <div class="info-value">
                        {{ form.companyName || '-' }}
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
                      <div class="info-label">対応エリア</div>

                      <div class="info-value">
                        {{ form.serviceArea || '-' }}
                      </div>
                    </div>

                    <div class="info-row">
                      <div class="info-label">資本金</div>

                      <div class="info-value">
                        {{
                          form.capitalAmount == null
                            ? '-'
                            : `${form.capitalAmount.toLocaleString()}円`
                        }}
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
      </v-card-text>

      <v-divider />

      <v-card-actions class="dialog-actions">
        <template v-if="editMode">
          <v-btn variant="text" :disabled="loading" @click="cancelEdit"> キャンセル </v-btn>

          <v-spacer />

          <v-btn
            color="primary"
            prepend-icon="mdi-content-save"
            :loading="saveMutation.isPending.value"
            @click="save"
          >
            保存
          </v-btn>
        </template>

        <template v-else>
          <v-spacer />

          <v-btn color="primary" variant="flat" @click="close"> 閉じる </v-btn>
        </template>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.company-dialog {
  height: 90vh;
  display: flex;
  flex-direction: column;
}

.company-dialog-title {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: rgb(var(--v-theme-primary));
  color: white;
}

.title-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-main {
  font-size: 18px;
  font-weight: 800;
}

.title-sub {
  margin-top: 2px;
  font-size: 12px;
  opacity: 0.8;
}

.dialog-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: #f8fafc;
}

.dialog-actions {
  padding: 16px 24px;
}

.form-section,
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
