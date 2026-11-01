<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'

import {
  deleteCalendarHoliday,
  getCalendarHolidays,
  saveCalendarHoliday,
} from '../api/calendarApi'
import type {
  CalendarHoliday,
  CalendarHolidaySaveRequest,
  CalendarType,
} from '../types/calendarTypes'

const currentYear = new Date().getFullYear()
const activeType = ref<CalendarType>('COMPANY')
const targetYear = ref(currentYear)
const holidays = ref<CalendarHoliday[]>([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const emptyCompanyHoliday = (): CalendarHolidaySaveRequest => ({
  id: null,
  calendarType: 'COMPANY',
  holidayDate: `${targetYear.value}-08-13`,
  holidayName: '',
  note: null,
  activeFlag: true,
})

const editing = reactive<CalendarHolidaySaveRequest>(emptyCompanyHoliday())

const isCompanyCalendar = computed(() => activeType.value === 'COMPANY')

const calendarDescription = computed(() => {
  switch (activeType.value) {
    case 'GOVERNMENT':
      return '官公庁への申請期限などで参照する共通カレンダーです。現在は参照専用です。'
    case 'FINANCIAL_INSTITUTION':
      return '給与・請求の支払日補正で参照する共通カレンダーです。現在は参照専用です。'
    case 'COMPANY':
      return '勤務日や社内業務で使用する会社独自の休日を設定します。'
    default:
      return ''
  }
})

const resetEditor = () => Object.assign(editing, emptyCompanyHoliday())

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    holidays.value = await getCalendarHolidays(activeType.value, Number(targetYear.value))
    resetEditor()
  } catch {
    errorMessage.value = 'カレンダーの取得に失敗しました。'
  } finally {
    loading.value = false
  }
}

const edit = (item: CalendarHoliday) => {
  if (!item.editable) return
  Object.assign(editing, {
    id: item.id,
    calendarType: item.calendarType,
    holidayDate: item.holidayDate,
    holidayName: item.holidayName,
    note: item.note,
    activeFlag: item.activeFlag,
  })
}

const save = async () => {
  if (!editing.holidayDate || !editing.holidayName.trim()) {
    errorMessage.value = '日付と休日名を入力してください。'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    await saveCalendarHoliday({
      ...editing,
      calendarType: 'COMPANY',
      holidayName: editing.holidayName.trim(),
    })
    successMessage.value = '会社休日を保存しました。'
    await load()
  } catch {
    errorMessage.value = '会社休日の保存に失敗しました。'
    loading.value = false
  }
}

const remove = async (item: CalendarHoliday) => {
  if (item.id == null || !item.editable) return
  if (!window.confirm(`「${item.holidayName}」を無効にしますか？`)) return
  loading.value = true
  try {
    await deleteCalendarHoliday(item.id)
    successMessage.value = '会社休日を無効にしました。'
    await load()
  } catch {
    errorMessage.value = '会社休日の無効化に失敗しました。'
    loading.value = false
  }
}

watch(activeType, () => void load())
onMounted(() => void load())
</script>

<template>
  <ListDetailPageLayout
    title="カレンダー管理"
    description="官公庁・金融機関・会社の休日を、業務機能から独立して管理します。"
  >
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>
    <v-alert
      v-if="successMessage"
      type="success"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="successMessage = ''"
    >
      {{ successMessage }}
    </v-alert>

    <v-card variant="outlined" :loading="loading">
      <v-tabs v-model="activeType" color="primary">
        <v-tab value="GOVERNMENT">官公庁休日</v-tab>
        <v-tab value="FINANCIAL_INSTITUTION">金融機関休日</v-tab>
        <v-tab value="COMPANY">会社休日</v-tab>
      </v-tabs>
      <v-divider />

      <section class="calendar-section">
        <div class="calendar-heading">
          <div>
            <h2>{{ activeType === 'COMPANY' ? '会社休日' : activeType === 'GOVERNMENT' ? '官公庁休日' : '金融機関休日' }}</h2>
            <p>{{ calendarDescription }}</p>
          </div>
          <div class="year-actions">
            <v-text-field
              v-model.number="targetYear"
              label="対象年"
              type="number"
              min="2000"
              max="2200"
              suffix="年"
              hide-details
            />
            <v-btn variant="tonal" :loading="loading" @click="load">表示</v-btn>
          </div>
        </div>

        <v-alert v-if="!isCompanyCalendar" type="info" variant="tonal" class="mb-4">
          このカレンダーはシステム共通データとして扱うため、この画面では編集できません。
        </v-alert>

        <div v-if="isCompanyCalendar" class="holiday-editor">
          <v-text-field v-model="editing.holidayDate" label="日付" type="date" hide-details />
          <v-text-field v-model="editing.holidayName" label="休日名" hide-details />
          <v-text-field v-model="editing.note" label="備考" hide-details />
          <v-switch v-model="editing.activeFlag" label="有効" color="primary" hide-details />
          <v-btn color="primary" :loading="loading" @click="save">保存</v-btn>
          <v-btn variant="text" @click="resetEditor">入力をクリア</v-btn>
        </div>

        <v-table density="comfortable">
          <thead>
            <tr>
              <th>日付</th>
              <th>休日名</th>
              <th>備考</th>
              <th>状態</th>
              <th v-if="isCompanyCalendar" />
            </tr>
          </thead>
          <tbody>
            <tr v-if="holidays.length === 0">
              <td :colspan="isCompanyCalendar ? 5 : 4" class="empty-row">
                対象年の休日は登録されていません。
              </td>
            </tr>
            <tr v-for="item in holidays" :key="item.id ?? `${item.calendarType}-${item.holidayDate}`">
              <td>{{ item.holidayDate }}</td>
              <td>{{ item.holidayName }}</td>
              <td>{{ item.note || '—' }}</td>
              <td>{{ item.activeFlag ? '有効' : '無効' }}</td>
              <td v-if="isCompanyCalendar" class="row-actions">
                <v-btn size="small" variant="text" @click="edit(item)">編集</v-btn>
                <v-btn size="small" color="error" variant="text" @click="remove(item)">無効化</v-btn>
              </td>
            </tr>
          </tbody>
        </v-table>
      </section>
    </v-card>
  </ListDetailPageLayout>
</template>

<style scoped>
.calendar-section {
  padding: 24px;
}

.calendar-heading,
.year-actions,
.holiday-editor,
.row-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.calendar-heading {
  justify-content: space-between;
  margin-bottom: 20px;
}

.calendar-heading h2,
.calendar-heading p {
  margin: 0;
}

.calendar-heading p {
  margin-top: 4px;
  color: rgb(var(--v-theme-on-surface-variant));
}

.year-actions {
  width: 320px;
}

.holiday-editor {
  display: grid;
  grid-template-columns: 180px minmax(180px, 1fr) minmax(220px, 1.4fr) auto auto auto;
  margin-bottom: 20px;
}

.row-actions {
  justify-content: flex-end;
}

.empty-row {
  padding: 32px !important;
  text-align: center;
  color: rgb(var(--v-theme-on-surface-variant));
}

@media (max-width: 960px) {
  .calendar-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .year-actions {
    width: 100%;
  }

  .holiday-editor {
    grid-template-columns: 1fr;
  }
}
</style>
