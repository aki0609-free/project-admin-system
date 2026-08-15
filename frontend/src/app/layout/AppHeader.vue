<script setup lang="ts">
import {
  ref,
  onMounted,
  onUnmounted,
} from 'vue'

import { useAuth } from '@/shared/auth/composables/useAuth'
import {
  externalSupportLinks,
  resolveHttpsUrl,
} from '@/shared/support/externalSupportLinks'
import { getExternalSupportLinks } from '@/shared/support/supportLinkApi'

import CompanyProfileDialog from '@/features/system/company/components/CompanyProfileDialog.vue'

const emit = defineEmits<{
  (e: 'toggle'): void
}>()

const {
  user,
  logout,
} = useAuth()

const now = ref('')
const companyDialog = ref(false)
const manualUrl = ref<string | null>(externalSupportLinks.manual)
const incidentFormUrl = ref<string | null>(externalSupportLinks.incident)

let timer: number | undefined

const formatDate = () => {
  const date = new Date()

  const yyyy =
    date.getFullYear()

  const mm =
    String(date.getMonth() + 1)
      .padStart(2, '0')

  const dd =
    String(date.getDate())
      .padStart(2, '0')

  const hh =
    String(date.getHours())
      .padStart(2, '0')

  const mi =
    String(date.getMinutes())
      .padStart(2, '0')

  now.value =
    `${yyyy}年${mm}月${dd}日 `
    + `${hh}時${mi}分`
}

const loadExternalSupportLinks = async () => {
  try {
    const setting = await getExternalSupportLinks()
    manualUrl.value = resolveHttpsUrl(setting.manualUrl) ?? externalSupportLinks.manual
    incidentFormUrl.value =
      resolveHttpsUrl(setting.incidentReportUrl) ?? externalSupportLinks.incident
  } catch {
    // DB設定を取得できない場合も、ビルド時の既定URLでメニューを利用可能にする。
  }
}

onMounted(() => {
  formatDate()
  void loadExternalSupportLinks()

  timer = window.setInterval(
    formatDate,
    1000,
  )
})

onUnmounted(() => {
  if (timer != null) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <v-app-bar
    elevation="1"
    color="white"
  >
    <v-app-bar-nav-icon
      @click="emit('toggle')"
    />

    <v-toolbar-title
      class="font-weight-bold text-primary text-h5"
    >
      HR Analytics &amp; Administration System
    </v-toolbar-title>

    <v-spacer />

    <div
      class="mr-6 text-body-2 text-grey-darken-1"
    >
      {{ now }}
    </div>

    <v-menu>
      <template #activator="{ props }">
        <v-btn
          v-bind="props"
          variant="text"
          prepend-icon="mdi-account-circle"
        >
          {{ user?.username }}
        </v-btn>
      </template>

      <v-list>
        <v-list-item
          title="会社情報"
          prepend-icon="mdi-domain"
          @click="companyDialog = true"
        />

        <v-list-item
          v-if="manualUrl"
          title="マニュアル"
          subtitle="Confluenceを開く"
          prepend-icon="mdi-book-open-page-variant-outline"
          :href="manualUrl"
          target="_blank"
          rel="noopener noreferrer"
        />

        <v-list-item
          v-if="incidentFormUrl"
          title="不具合・Incident報告"
          subtitle="Jira Formsを開く"
          prepend-icon="mdi-bug-outline"
          :href="incidentFormUrl"
          target="_blank"
          rel="noopener noreferrer"
        />

        <v-divider />

        <v-list-item
          title="ログアウト"
          prepend-icon="mdi-logout"
          @click="logout"
        />
      </v-list>
    </v-menu>
  </v-app-bar>

  <CompanyProfileDialog
    v-model="companyDialog"
  />
</template>
