<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, provide, ref, watch } from 'vue'
import {
  ContextMenu,
  DetailsView,
  FileManagerComponent as EjsFilemanager,
  NavigationPane,
  Toolbar,
  type BeforeDownloadEventArgs,
  type BeforeSendEventArgs,
  type ContextMenuSettingsModel,
  type ToolbarSettingsModel,
} from '@syncfusion/ej2-vue-filemanager'
import axiosApiClient from '@/app/plugins/axiosApiClient'
import { configureSyncfusion } from '@/app/plugins/syncfusion'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import { configureFileManagerJapaneseLocale } from '../i18n/fileManagerJa'
import type { DocumentArea, DocumentAreaResponse } from '../types/documentTypes'

import '@syncfusion/ej2-base/styles/material3.css'
import '@syncfusion/ej2-buttons/styles/material3.css'
import '@syncfusion/ej2-inputs/styles/material3.css'
import '@syncfusion/ej2-popups/styles/material3.css'
import '@syncfusion/ej2-splitbuttons/styles/material3.css'
import '@syncfusion/ej2-navigations/styles/material3.css'
import '@syncfusion/ej2-layouts/styles/material3.css'
import '@syncfusion/ej2-grids/styles/material3.css'
import '@syncfusion/ej2-vue-filemanager/styles/material3.css'

interface AjaxRequestArgs {
  httpRequest: XMLHttpRequest
}

interface MutableAjaxSettings {
  beforeSend?: (args: AjaxRequestArgs) => void
}

interface FileManagerResultEvent {
  action?: string
}

interface FileManagerFailureEvent {
  error?: {
    message?: string
  }
}

const areaMetadata: Record<DocumentArea, { icon: string; description: string }> = {
  GENERAL: {
    icon: 'mdi-folder-edit-outline',
    description: '契約書や社内資料などの会社書類を保管できます。',
  },
  GENERATED_REPORTS: {
    icon: 'mdi-file-document-multiple-outline',
    description: 'システムが生成した帳票を参照・ダウンロードできます。',
  },
  BACKUPS: {
    icon: 'mdi-archive-outline',
    description: '年度帳票とシステムバックアップを参照できます。',
  },
  TEMPLATES: {
    icon: 'mdi-file-cog-outline',
    description: '台帳・帳票テンプレートを参照できます。',
  },
  IMPORT_SCRIPTS: {
    icon: 'mdi-language-python',
    description: '外部データ取込で実行するPython・Shellスクリプトを管理します。',
  },
}

const defaultArea: DocumentAreaResponse = {
  area: 'GENERAL',
  displayName: '会社書類',
  allowedOperations: [
    'READ',
    'SEARCH',
    'DETAILS',
    'DOWNLOAD',
    'CREATE_DIRECTORY',
    'UPLOAD',
    'COPY',
    'MOVE',
    'RENAME',
    'DELETE',
  ],
}

const fallbackAreas: DocumentAreaResponse[] = [
  defaultArea,
  {
    area: 'GENERATED_REPORTS',
    displayName: '生成帳票',
    allowedOperations: ['READ', 'SEARCH', 'DETAILS', 'DOWNLOAD'],
  },
  {
    area: 'BACKUPS',
    displayName: 'バックアップ',
    allowedOperations: ['READ', 'SEARCH', 'DETAILS', 'DOWNLOAD'],
  },
  {
    area: 'TEMPLATES',
    displayName: 'テンプレート',
    allowedOperations: ['READ', 'SEARCH', 'DETAILS', 'DOWNLOAD'],
  },
  {
    area: 'IMPORT_SCRIPTS',
    displayName: '取込スクリプト',
    allowedOperations: [
      'READ',
      'SEARCH',
      'DETAILS',
      'DOWNLOAD',
      'CREATE_DIRECTORY',
      'UPLOAD',
      'COPY',
      'MOVE',
      'RENAME',
      'DELETE',
    ],
  },
]

const licenseRegistered = configureSyncfusion()
configureFileManagerJapaneseLocale()
provide('filemanager', [NavigationPane, DetailsView, Toolbar, ContextMenu])

const areas = ref<DocumentAreaResponse[]>(fallbackAreas)
const selectedArea = ref<DocumentArea>('GENERAL')
const loadingAreas = ref(false)
const areaLoadError = ref(false)
const operationMessage = ref('')
const operationError = ref('')
let operationMessageTimer: ReturnType<typeof setTimeout> | undefined

const currentArea = computed<DocumentAreaResponse>(
  () => areas.value.find((area) => area.area === selectedArea.value) ?? defaultArea,
)
const editable = computed(() => currentArea.value.allowedOperations.includes('UPLOAD'))
const currentMetadata = computed(() => areaMetadata[selectedArea.value])

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const fileManagerBaseUrl = computed(
  () => `${apiBaseUrl}/api/admin/documents/file-manager/${selectedArea.value}`,
)
const ajaxSettings = computed(() => ({
  url: `${fileManagerBaseUrl.value}/operations`,
  uploadUrl: `${fileManagerBaseUrl.value}/upload`,
  downloadUrl: `${fileManagerBaseUrl.value}/download`,
}))

const toolbarSettings = computed<ToolbarSettingsModel>(() => ({
  visible: true,
  items: editable.value
    ? [
        'NewFolder',
        'Upload',
        'Cut',
        'Copy',
        'Paste',
        'Delete',
        'Download',
        'Rename',
        'SortBy',
        'Refresh',
        'Selection',
        'View',
        'Details',
      ]
    : ['Download', 'SortBy', 'Refresh', 'Selection', 'View', 'Details'],
}))

const contextMenuSettings = computed<ContextMenuSettingsModel>(() => ({
  visible: true,
  file: editable.value
    ? ['Open', '|', 'Cut', 'Copy', '|', 'Delete', 'Rename', '|', 'Download', 'Details']
    : ['Open', '|', 'Download', 'Details'],
  folder: editable.value
    ? ['Open', '|', 'Cut', 'Copy', 'Paste', '|', 'Delete', 'Rename', '|', 'Download', 'Details']
    : ['Open', '|', 'Download', 'Details'],
  layout: editable.value
    ? [
        'SortBy',
        'View',
        'Refresh',
        '|',
        'Paste',
        '|',
        'NewFolder',
        'Upload',
        '|',
        'Details',
        'SelectAll',
      ]
    : ['SortBy', 'View', 'Refresh', '|', 'Details', 'SelectAll'],
}))

const detailsViewSettings = {
  columnResizing: true,
  columns: [
    {
      field: 'name',
      headerText: '名前',
      minWidth: 120,
      template: '<span class="e-fe-text">${name}</span>',
      customAttributes: { class: 'e-fe-grid-name' },
    },
    {
      field: '_fm_modified',
      headerText: '更新日時',
      type: 'dateTime',
      format: 'yyyy年M月d日 HH:mm',
      minWidth: 150,
      width: '210',
    },
    {
      field: 'size',
      headerText: 'サイズ',
      minWidth: 90,
      width: '110',
      template: '<span class="e-fe-size">${size}</span>',
      format: 'n2',
    },
  ],
}

function setAuthorizationHeader(args: BeforeSendEventArgs | BeforeDownloadEventArgs) {
  const settings = args.ajaxSettings as MutableAjaxSettings | undefined
  if (!settings) return

  settings.beforeSend = (request) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      request.httpRequest.setRequestHeader('Authorization', `Bearer ${token}`)
    }
    request.httpRequest.setRequestHeader('X-Tenant-ID', 'default')
  }
}

function handleBeforeSend(args: BeforeSendEventArgs) {
  setAuthorizationHeader(args)
}

function handleBeforeDownload(args: BeforeDownloadEventArgs) {
  args.useFormPost = false
  setAuthorizationHeader(args)
}

function handleSuccess(args: FileManagerResultEvent) {
  if (!args.action || ['read', 'search'].includes(args.action)) return

  const messages: Record<string, string> = {
    create: 'フォルダーを作成しました。',
    delete: '選択項目を削除しました。',
    rename: '名前を変更しました。',
    move: '選択項目を移動しました。',
    copy: '選択項目をコピーしました。',
    upload: 'ファイルをアップロードしました。',
    download: 'ダウンロードを開始しました。',
  }
  operationMessage.value = messages[args.action] ?? ''
  operationError.value = ''
  if (!operationMessage.value) return
  if (operationMessageTimer) clearTimeout(operationMessageTimer)
  operationMessageTimer = setTimeout(() => {
    operationMessage.value = ''
    operationMessageTimer = undefined
  }, 4000)
}

function handleFailure(args: FileManagerFailureEvent) {
  operationMessage.value = ''
  operationError.value = args.error?.message || '書類の操作に失敗しました。'
}

async function loadAreas() {
  loadingAreas.value = true
  areaLoadError.value = false

  try {
    const response = await axiosApiClient.get<DocumentAreaResponse[]>('/api/admin/documents/areas')
    areas.value = response.data
  } catch {
    areaLoadError.value = true
    areas.value = fallbackAreas
  } finally {
    loadingAreas.value = false
  }
}

onMounted(loadAreas)
watch(selectedArea, () => {
  operationMessage.value = ''
  operationError.value = ''
})
onBeforeUnmount(() => {
  if (operationMessageTimer) clearTimeout(operationMessageTimer)
})
</script>

<template>
  <ListDetailPageLayout
    class="document-management-page"
    title="書類管理"
    description="会社書類、生成帳票、バックアップ、テンプレート、取込スクリプトを一元管理します。"
  >
    <template #header-actions>
      <v-chip color="primary" variant="tonal" prepend-icon="mdi-shield-account-outline">
        SYS_ADMIN専用
      </v-chip>
    </template>

    <v-alert
      v-if="!licenseRegistered"
      type="warning"
      variant="tonal"
      density="compact"
      class="mb-4"
    >
      Syncfusionライセンスキーが未設定です。
      VITE_SYNCFUSION_LICENSE_KEYを設定して再起動してください。
    </v-alert>

    <v-alert v-if="areaLoadError" type="warning" variant="tonal" density="compact" class="mb-4">
      領域設定を取得できなかったため、既定の権限表示を使用しています。
      <template #append>
        <v-btn variant="text" size="small" @click="loadAreas">再取得</v-btn>
      </template>
    </v-alert>

    <v-alert
      v-if="operationMessage"
      type="success"
      variant="tonal"
      density="compact"
      closable
      class="mb-4"
      @click:close="operationMessage = ''"
    >
      {{ operationMessage }}
    </v-alert>

    <v-alert
      v-if="operationError"
      type="error"
      variant="tonal"
      density="compact"
      closable
      class="mb-4"
      @click:close="operationError = ''"
    >
      {{ operationError }}
    </v-alert>

    <v-card variant="outlined" class="mb-4">
      <v-card-text>
        <div class="text-subtitle-2 mb-3">書類領域</div>
        <v-btn-toggle
          v-model="selectedArea"
          color="primary"
          mandatory
          divided
          class="document-management-page__areas"
          :disabled="loadingAreas"
        >
          <v-btn
            v-for="area in areas"
            :key="area.area"
            :value="area.area"
            :prepend-icon="areaMetadata[area.area].icon"
          >
            {{ area.displayName }}
          </v-btn>
        </v-btn-toggle>

        <div class="d-flex flex-wrap align-center ga-2 mt-3">
          <span class="text-body-2">
            {{ currentMetadata.description }}
          </span>
          <v-chip size="small" :color="editable ? 'success' : 'secondary'" variant="tonal">
            {{ editable ? '編集可能' : '参照専用' }}
          </v-chip>
          <span class="text-caption text-medium-emphasis">
            {{ selectedArea === 'IMPORT_SCRIPTS' ? '.py/.sh・最大1MB' : '1ファイルあたり最大50MB' }}
          </span>
        </div>
        <v-alert
          v-if="editable"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-3"
        >
          同じ場所に同名のファイルは登録できません。差し替える場合は、既存ファイルを名称変更または削除してからアップロードしてください。
        </v-alert>
        <v-alert
          v-if="selectedArea === 'IMPORT_SCRIPTS'"
          type="warning"
          variant="tonal"
          density="compact"
          class="mt-3"
        >
          登録したスクリプトは外部データ取込から実行できます。内容を確認した管理者だけが更新してください。
        </v-alert>
      </v-card-text>
    </v-card>

    <v-card variant="outlined">
      <v-card-text class="pa-0">
        <EjsFilemanager
          :id="`project-admin-file-manager-${selectedArea}`"
          :key="selectedArea"
          locale="ja"
          height="640px"
          width="100%"
          view="Details"
          :root-alias-name="currentArea.displayName"
          :ajax-settings="ajaxSettings"
          :toolbar-settings="toolbarSettings"
          :context-menu-settings="contextMenuSettings"
          :details-view-settings="detailsViewSettings"
          :navigation-pane-settings="{ visible: true, minWidth: '220px' }"
          :upload-settings="{
            autoUpload: true,
            maxFileSize: selectedArea === 'IMPORT_SCRIPTS' ? 1024 * 1024 : 50 * 1024 * 1024,
            minFileSize: 1,
          }"
          :allow-drag-and-drop="editable"
          :show-thumbnail="false"
          :show-file-extension="true"
          :enable-html-sanitizer="true"
          :before-send="handleBeforeSend"
          :before-download="handleBeforeDownload"
          :success="handleSuccess"
          :failure="handleFailure"
        />
      </v-card-text>
    </v-card>
  </ListDetailPageLayout>
</template>

<style scoped>
.document-management-page {
  max-width: 1800px;
}

.document-management-page__areas {
  flex-wrap: wrap;
  height: auto;
}

.document-management-page :deep(.e-filemanager) {
  border: 0;
}

.document-management-page :deep(.e-toolbar) {
  border-top: 0;
  border-right: 0;
  border-left: 0;
}
</style>
