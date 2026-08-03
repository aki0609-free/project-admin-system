<script setup lang="ts">
import { computed, onMounted, provide, ref } from 'vue'
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
import { configureFileManagerJapaneseLocale } from '../i18n/fileManagerJa'
import type {
  DocumentArea,
  DocumentAreaResponse,
} from '../types/documentTypes'

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

const areaMetadata: Record<
  DocumentArea,
  { icon: string; description: string }
> = {
  GENERAL: {
    icon: 'mdi-folder-edit-outline',
    description: '契約書や社内資料などを自由に保管できます。',
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
}

const defaultArea: DocumentAreaResponse = {
  area: 'GENERAL',
  displayName: '自由書類',
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
]

const licenseRegistered = configureSyncfusion()
configureFileManagerJapaneseLocale()
provide('filemanager', [NavigationPane, DetailsView, Toolbar, ContextMenu])

const areas = ref<DocumentAreaResponse[]>(fallbackAreas)
const selectedArea = ref<DocumentArea>('GENERAL')
const loadingAreas = ref(false)
const areaLoadError = ref(false)
const operationMessage = ref('')

const currentArea = computed<DocumentAreaResponse>(
  () =>
    areas.value.find(area => area.area === selectedArea.value)
    ?? defaultArea,
)
const editable = computed(() =>
  currentArea.value.allowedOperations.includes('UPLOAD'),
)
const currentMetadata = computed(() => areaMetadata[selectedArea.value])

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '')
  .replace(/\/$/, '')
const fileManagerBaseUrl = computed(
  () =>
    `${apiBaseUrl}/api/admin/documents/file-manager/${selectedArea.value}`,
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
    : [
        'Download',
        'SortBy',
        'Refresh',
        'Selection',
        'View',
        'Details',
      ],
}))

const contextMenuSettings = computed<ContextMenuSettingsModel>(() => ({
  visible: true,
  file: editable.value
    ? [
        'Open',
        '|',
        'Cut',
        'Copy',
        '|',
        'Delete',
        'Rename',
        '|',
        'Download',
        'Details',
      ]
    : ['Open', '|', 'Download', 'Details'],
  folder: editable.value
    ? [
        'Open',
        '|',
        'Cut',
        'Copy',
        'Paste',
        '|',
        'Delete',
        'Rename',
        '|',
        'Download',
        'Details',
      ]
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

function setAuthorizationHeader(
  args: BeforeSendEventArgs | BeforeDownloadEventArgs,
) {
  const settings = args.ajaxSettings as MutableAjaxSettings | undefined
  if (!settings) return

  settings.beforeSend = request => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      request.httpRequest.setRequestHeader(
        'Authorization',
        `Bearer ${token}`,
      )
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
    download: 'ダウンロードを開始しました。',
  }
  operationMessage.value = messages[args.action] ?? ''
}

async function loadAreas() {
  loadingAreas.value = true
  areaLoadError.value = false

  try {
    const response = await axiosApiClient.get<DocumentAreaResponse[]>(
      '/api/admin/documents/areas',
    )
    areas.value = response.data
  } catch {
    areaLoadError.value = true
    areas.value = fallbackAreas
  } finally {
    loadingAreas.value = false
  }
}

onMounted(loadAreas)
</script>

<template>
  <v-container fluid class="document-management-page pa-4">
    <div class="d-flex flex-wrap align-center ga-3 mb-4">
      <div>
        <h1 class="text-h5 font-weight-bold">書類管理</h1>
        <p class="text-body-2 text-medium-emphasis mt-1">
          自由書類、生成帳票、バックアップ、テンプレートを一元管理します。
        </p>
      </div>
      <v-spacer />
      <v-chip
        color="primary"
        variant="tonal"
        prepend-icon="mdi-shield-account-outline"
      >
        SYS_ADMIN専用
      </v-chip>
    </div>

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

    <v-alert
      v-if="areaLoadError"
      type="warning"
      variant="tonal"
      density="compact"
      class="mb-4"
    >
      領域設定を取得できなかったため、既定の権限表示を使用しています。
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
          <v-chip
            size="small"
            :color="editable ? 'success' : 'secondary'"
            variant="tonal"
          >
            {{ editable ? '編集可能' : '参照専用' }}
          </v-chip>
          <span class="text-caption text-medium-emphasis">
            1ファイルあたり最大50MB
          </span>
        </div>
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
          :navigation-pane-settings="{ visible: true, minWidth: '220px' }"
          :upload-settings="{
            autoUpload: true,
            maxFileSize: 50 * 1024 * 1024,
            minFileSize: 1,
          }"
          :allow-drag-and-drop="editable"
          :show-thumbnail="false"
          :show-file-extension="true"
          :enable-html-sanitizer="true"
          :before-send="handleBeforeSend"
          :before-download="handleBeforeDownload"
          :success="handleSuccess"
        />
      </v-card-text>
    </v-card>
  </v-container>
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
