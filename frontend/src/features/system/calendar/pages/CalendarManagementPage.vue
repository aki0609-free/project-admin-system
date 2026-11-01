<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'

import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'

import {
  deleteCalendarHoliday,
  getCalendarEffectiveHolidays,
  getCalendarHolidays,
  getCalendarPolicy,
  saveCalendarHoliday,
  saveCalendarPolicy,
} from '../api/calendarApi'
import type {
  CalendarHoliday,
  CalendarEffectiveHoliday,
  CalendarHolidaySource,
  CalendarHolidaySaveRequest,
  CalendarPolicy,
  CalendarType,
  CalendarWeekday,
} from '../types/calendarTypes'

const currentYear = new Date().getFullYear()
const activeType = ref<CalendarType>('COMPANY')
const targetYear = ref(currentYear)
const holidays = ref<CalendarHoliday[]>([])
const effectiveHolidays = ref<CalendarEffectiveHoliday[]>([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const policy = ref<CalendarPolicy | null>(null)

const weekdayOptions: { value: CalendarWeekday; label: string }[] = [
  { value: 'MONDAY', label: '月' },
  { value: 'TUESDAY', label: '火' },
  { value: 'WEDNESDAY', label: '水' },
  { value: 'THURSDAY', label: '木' },
  { value: 'FRIDAY', label: '金' },
  { value: 'SATURDAY', label: '土' },
  { value: 'SUNDAY', label: '日' },
]

const emptyCompanyHoliday = (): CalendarHolidaySaveRequest => ({
  id: null,
  calendarType: 'COMPANY',
  holidayDate: `${targetYear.value}-08-13`,
  holidayName: '',
  note: null,
  activeFlag: true,
  dayType: 'CLOSED',
})

const editing = reactive<CalendarHolidaySaveRequest>(emptyCompanyHoliday())

const isCompanyCalendar = computed(() => activeType.value === 'COMPANY')
const isSharedReferenceCalendar = computed(() =>
  activeType.value === 'GOVERNMENT'
  || activeType.value === 'FINANCIAL_INSTITUTION',
)

const calendarTypeLabel = (type: CalendarType) => {
  switch (type) {
    case 'NATIONAL_HOLIDAY': return '国民の祝日'
    case 'GOVERNMENT': return '官公庁休日'
    case 'FINANCIAL_INSTITUTION': return '金融機関休日'
    case 'COMPANY': return '会社休日'
  }
}

const calendarTitle = computed(() => {
  switch (activeType.value) {
    case 'NATIONAL_HOLIDAY':
      return '国民の祝日'
    case 'GOVERNMENT':
      return '官公庁休日'
    case 'FINANCIAL_INSTITUTION':
      return '金融機関休日'
    case 'COMPANY':
      return '会社休日'
    default:
      return ''
  }
})

const calendarDescription = computed(() => {
  switch (activeType.value) {
    case 'NATIONAL_HOLIDAY':
      return '内閣府の公式CSVを外部データ取込から登録する、共通の祝日カレンダーです。'
    case 'GOVERNMENT':
      return '国民の祝日、土日、12月29日から1月3日を基準とする参照専用カレンダーです。'
    case 'FINANCIAL_INSTITUTION':
      return '国民の祝日、土日、12月31日から1月3日を基準とする参照専用カレンダーです。'
    case 'COMPANY':
      return '勤務日や社内業務で使用する会社独自の休日を設定します。'
    default:
      return ''
  }
})

const holidayListTitle = computed(() => {
  if (activeType.value === 'NATIONAL_HOLIDAY') return '登録済みの国民の祝日'
  if (isSharedReferenceCalendar.value) return '個別追加休日'
  return '個別の休日・営業日'
})

const emptyHolidayMessage = computed(() => {
  if (activeType.value === 'NATIONAL_HOLIDAY') {
    return '対象年の国民の祝日は登録されていません。'
  }
  if (isSharedReferenceCalendar.value) {
    return `対象年に個別追加された${calendarTitle.value}はありません。`
  }
  return '対象年の個別休日・営業日は登録されていません。'
})

const resetEditor = () => Object.assign(editing, emptyCompanyHoliday())

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [holidayRows, calendarPolicy, effectiveRows] = await Promise.all([
      getCalendarHolidays(activeType.value, Number(targetYear.value)),
      getCalendarPolicy(activeType.value),
      isSharedReferenceCalendar.value
        ? getCalendarEffectiveHolidays(activeType.value, Number(targetYear.value))
        : Promise.resolve([]),
    ])
    holidays.value = holidayRows
    policy.value = calendarPolicy
    effectiveHolidays.value = effectiveRows
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
    dayType: item.dayType,
  })
}

const savePolicy = async () => {
  if (!policy.value || !isCompanyCalendar.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    policy.value = await saveCalendarPolicy({
      calendarType: 'COMPANY',
      closedWeekdays: policy.value.closedWeekdays,
      includedCalendarTypes: policy.value.includedCalendarTypes,
    })
    successMessage.value = '会社休日のルールを保存しました。'
  } catch {
    errorMessage.value = '会社休日のルール保存に失敗しました。'
  } finally {
    loading.value = false
  }
}

const formatJapaneseDate = (value: string) => {
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) return value
  return `${year}年${month}月${day}日`
}

const formatMonthDay = (value: string) => {
  const [month, day] = value.split('-').map(Number)
  if (!month || !day) return value
  return `${month}月${day}日`
}

const sourceLabel = (source: CalendarHolidaySource) => {
  switch (source) {
    case 'WEEKDAY_RULE': return '曜日ルール'
    case 'NATIONAL_HOLIDAY': return '国民の祝日'
    case 'YEAR_END_NEW_YEAR': return '年末年始'
    case 'INDIVIDUAL': return '個別追加'
  }
}

const sourceColor = (source: CalendarHolidaySource) => {
  switch (source) {
    case 'NATIONAL_HOLIDAY': return 'primary'
    case 'YEAR_END_NEW_YEAR': return 'warning'
    case 'INDIVIDUAL': return 'success'
    default: return undefined
  }
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

    <v-card variant="outlined" :loading="loading" class="calendar-shell">
      <v-tabs v-model="activeType" color="primary">
        <v-tab value="NATIONAL_HOLIDAY">国民の祝日</v-tab>
        <v-tab value="GOVERNMENT">官公庁休日</v-tab>
        <v-tab value="FINANCIAL_INSTITUTION">金融機関休日</v-tab>
        <v-tab value="COMPANY">会社休日</v-tab>
      </v-tabs>
      <v-divider />

      <section class="calendar-section">
        <div class="calendar-heading">
          <div>
            <h2>{{ calendarTitle }}</h2>
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

        <v-alert v-if="!isCompanyCalendar" type="info" variant="outlined" class="mb-4">
          <template v-if="activeType === 'NATIONAL_HOLIDAY'">
            「外部データ取込」の「国民の祝日取込」から内閣府CSVを登録してください。
          </template>
          <template v-else>
            適用ルールと国民の祝日を合成した休日を表示しています。個別追加分以外は編集できません。
          </template>
        </v-alert>

        <v-card
          v-if="policy && activeType !== 'NATIONAL_HOLIDAY'"
          variant="outlined"
          class="settings-card policy-card"
        >
          <v-card-title class="settings-title">休日ルール</v-card-title>
          <v-divider />
          <v-card-text>
            <div class="policy-grid">
              <div class="policy-block">
                <div class="setting-label">定休日</div>
                <div class="setting-description">
                  {{ isCompanyCalendar ? '会社の通常の休日とする曜日を選択します。' : '毎週、休日として適用される曜日です。' }}
                </div>
                <v-chip-group
                  v-if="isCompanyCalendar"
                  v-model="policy.closedWeekdays"
                  multiple
                  selected-class="weekday-selected"
                  class="weekday-group"
                >
                  <v-chip
                    v-for="weekday in weekdayOptions"
                    :key="weekday.value"
                    :value="weekday.value"
                    filter
                    variant="outlined"
                    class="weekday-chip"
                  >
                    {{ weekday.label }}
                  </v-chip>
                </v-chip-group>
                <div v-else class="weekday-group weekday-readonly-group">
                  <v-chip
                    v-for="weekday in weekdayOptions"
                    :key="weekday.value"
                    variant="outlined"
                    class="weekday-chip"
                    :class="{
                      'weekday-selected': policy.closedWeekdays.includes(weekday.value),
                      'weekday-muted': !policy.closedWeekdays.includes(weekday.value),
                    }"
                  >
                    {{ weekday.label }}
                  </v-chip>
                </div>
              </div>
              <div v-if="!isCompanyCalendar" class="policy-block policy-separated-block">
                <div class="setting-label">共有する休日</div>
                <div class="setting-description">共通カレンダーから自動的に適用されます。</div>
                <div class="rule-chip-group">
                  <v-chip
                    v-for="includedType in policy.includedCalendarTypes"
                    :key="includedType"
                    color="primary"
                    variant="tonal"
                  >
                    {{ calendarTypeLabel(includedType) }}
                  </v-chip>
                </div>
              </div>
              <div v-if="!isCompanyCalendar" class="policy-block policy-separated-block">
                <div class="setting-label">年末年始</div>
                <div class="setting-description">このカレンダー固有の固定期間です。</div>
                <div class="fixed-period-list">
                  <div v-for="period in policy.fixedPeriods" :key="period.name" class="fixed-period-row">
                    <span>{{ period.name }}</span>
                    <strong>
                      {{ formatMonthDay(period.startMonthDay) }}～{{ formatMonthDay(period.endMonthDay) }}
                    </strong>
                  </div>
                </div>
              </div>
              <div v-if="isCompanyCalendar" class="policy-block policy-reference-block">
                <div class="setting-label">基準にする休日</div>
                <div class="setting-description">選択した共通カレンダーの休日を会社休日にも反映します。</div>
                <v-checkbox
                  v-model="policy.includedCalendarTypes"
                  value="GOVERNMENT"
                  label="官公庁休日に合わせる"
                  hide-details
                />
                <v-checkbox
                  v-model="policy.includedCalendarTypes"
                  value="FINANCIAL_INSTITUTION"
                  label="金融機関休日に合わせる"
                  hide-details
                />
              </div>
            </div>
          </v-card-text>
          <v-card-actions v-if="isCompanyCalendar">
            <v-spacer />
            <v-btn color="primary" :loading="loading" @click="savePolicy">
              休日ルールを保存
            </v-btn>
          </v-card-actions>
        </v-card>

        <v-card
          v-if="isSharedReferenceCalendar"
          variant="outlined"
          class="settings-card effective-card"
        >
          <v-card-title class="settings-title">
            {{ targetYear }}年に適用される休日
          </v-card-title>
          <v-card-subtitle class="settings-subtitle">
            曜日、国民の祝日、年末年始、個別追加を合成した実際の休日です。
          </v-card-subtitle>
          <v-divider />
          <div class="effective-table-wrapper">
            <v-table density="compact" fixed-header>
              <thead>
                <tr>
                  <th>日付</th>
                  <th>休日名</th>
                  <th>適用元</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="effectiveHolidays.length === 0">
                  <td colspan="3" class="empty-row">対象年に適用される休日はありません。</td>
                </tr>
                <tr
                  v-for="item in effectiveHolidays"
                  :key="`effective-${item.holidayDate}`"
                >
                  <td>{{ formatJapaneseDate(item.holidayDate) }}</td>
                  <td>{{ item.holidayName }}</td>
                  <td>
                    <div class="source-chips">
                      <v-chip
                        v-for="source in item.sources"
                        :key="source"
                        :color="sourceColor(source)"
                        size="small"
                        variant="tonal"
                      >
                        {{ sourceLabel(source) }}
                      </v-chip>
                    </div>
                  </td>
                </tr>
              </tbody>
            </v-table>
          </div>
        </v-card>

        <v-card v-if="isCompanyCalendar" variant="outlined" class="settings-card editor-card">
          <v-card-title class="settings-title">個別の休日・営業日</v-card-title>
          <v-divider />
          <v-card-text>
            <div class="holiday-editor">
              <v-text-field v-model="editing.holidayDate" label="日付" type="date" hide-details />
              <v-select
                v-model="editing.dayType"
                label="区分"
                :items="[
                  { title: '休日', value: 'CLOSED' },
                  { title: '営業日', value: 'OPEN' },
                ]"
                hide-details
              />
              <v-text-field v-model="editing.holidayName" label="名称" hide-details />
              <v-text-field v-model="editing.note" label="備考" hide-details />
              <v-switch v-model="editing.activeFlag" label="有効" color="primary" hide-details />
            </div>
          </v-card-text>
          <v-card-actions class="editor-actions">
            <v-btn variant="text" @click="resetEditor">入力をクリア</v-btn>
            <v-btn color="primary" :loading="loading" @click="save">保存</v-btn>
          </v-card-actions>
        </v-card>

        <v-card variant="outlined" class="settings-card holiday-list-card">
          <v-card-title class="settings-title">{{ holidayListTitle }}</v-card-title>
          <v-divider />
          <v-table density="comfortable">
            <thead>
              <tr>
                <th>日付</th>
                <th>区分</th>
                <th>休日名</th>
                <th>備考</th>
                <th>状態</th>
                <th v-if="isCompanyCalendar" />
              </tr>
            </thead>
            <tbody>
              <tr v-if="holidays.length === 0">
                <td :colspan="isCompanyCalendar ? 6 : 5" class="empty-row">
                  {{ emptyHolidayMessage }}
                </td>
              </tr>
              <tr v-for="item in holidays" :key="item.id ?? `${item.calendarType}-${item.holidayDate}`">
                <td>{{ formatJapaneseDate(item.holidayDate) }}</td>
                <td>{{ item.dayType === 'OPEN' ? '営業日' : '休日' }}</td>
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
        </v-card>
      </section>
    </v-card>
  </ListDetailPageLayout>
</template>

<style scoped>
.calendar-section {
  padding: 24px;
  background: #fff;
}

.calendar-shell {
  background: #fff;
  border-color: rgba(var(--v-theme-on-surface), 0.28);
  color: rgb(var(--v-theme-on-surface));
}

.calendar-shell :deep(.v-tabs),
.calendar-shell :deep(.v-tabs-window),
.calendar-shell :deep(.v-card-text),
.calendar-shell :deep(.v-table),
.calendar-shell :deep(.v-table__wrapper) {
  background: #fff;
}

.calendar-shell :deep(.v-tab) {
  color: rgba(var(--v-theme-on-surface), 0.78);
  opacity: 1;
}

.calendar-shell :deep(.v-tab.v-tab--selected) {
  color: rgb(var(--v-theme-primary));
  font-weight: 600;
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

.calendar-heading h2 {
  color: rgb(var(--v-theme-on-surface));
}

.calendar-heading p {
  margin-top: 4px;
  color: rgba(var(--v-theme-on-surface), 0.76);
  font-weight: 500;
}

.year-actions {
  width: 320px;
}

.holiday-editor {
  display: grid;
  grid-template-columns: 180px 120px minmax(180px, 1fr) minmax(220px, 1.4fr) auto;
  gap: 12px;
}

.policy-card {
  margin-bottom: 20px;
}

.editor-card {
  margin-bottom: 24px;
}

.settings-card {
  background: #fff;
  border-color: rgba(var(--v-theme-on-surface), 0.28);
  border-radius: 12px;
  box-shadow: none;
}

.settings-title {
  padding: 16px 20px;
  color: rgb(var(--v-theme-on-surface));
  font-size: 1rem;
  font-weight: 700;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0;
}

.policy-block {
  min-width: 0;
  padding: 4px 20px 4px 4px;
}

.policy-reference-block {
  padding-right: 4px;
  padding-left: 28px;
  border-left: 1px solid rgba(var(--v-theme-on-surface), 0.2);
}

.policy-separated-block {
  padding-left: 28px;
  border-left: 1px solid rgba(var(--v-theme-on-surface), 0.2);
}

.rule-chip-group,
.source-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.fixed-period-list {
  display: grid;
  gap: 8px;
}

.fixed-period-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border: 1px solid rgba(var(--v-theme-on-surface), 0.2);
  border-radius: 8px;
  color: rgb(var(--v-theme-on-surface));
}

.settings-subtitle {
  padding: 0 20px 14px;
  color: rgba(var(--v-theme-on-surface), 0.72);
  opacity: 1;
}

.effective-card,
.holiday-list-card {
  margin-bottom: 24px;
}

.effective-table-wrapper {
  max-height: 520px;
  overflow-y: auto;
}

.setting-label {
  margin-bottom: 4px;
  color: rgb(var(--v-theme-on-surface));
  font-weight: 600;
}

.setting-description {
  margin-bottom: 12px;
  color: rgba(var(--v-theme-on-surface), 0.72);
  font-size: 0.875rem;
}

.weekday-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.weekday-readonly-group {
  padding: 4px;
}

.weekday-chip {
  min-width: 42px;
  justify-content: center;
  background: #fff;
  border-color: rgba(var(--v-theme-on-surface), 0.35);
  color: rgba(var(--v-theme-on-surface), 0.88);
}

.weekday-chip.weekday-selected {
  background: rgba(var(--v-theme-primary), 0.12);
  border-color: rgb(var(--v-theme-primary));
  color: rgb(var(--v-theme-primary));
  font-weight: 600;
}

.weekday-chip.weekday-muted {
  color: rgba(var(--v-theme-on-surface), 0.58);
}

.editor-actions {
  justify-content: flex-end;
  padding: 0 20px 16px;
}

.settings-card :deep(.v-field-label),
.settings-card :deep(.v-label) {
  color: rgba(var(--v-theme-on-surface), 0.76);
  opacity: 1;
}

.settings-card :deep(.v-field__input),
.settings-card :deep(.v-selection-control__input) {
  color: rgb(var(--v-theme-on-surface));
}

.row-actions {
  justify-content: flex-end;
}

.empty-row {
  padding: 32px !important;
  text-align: center;
  color: rgba(var(--v-theme-on-surface), 0.72);
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

  .policy-grid {
    grid-template-columns: 1fr;
  }

  .policy-block,
  .policy-reference-block,
  .policy-separated-block {
    padding: 4px;
  }

  .policy-reference-block,
  .policy-separated-block {
    padding-top: 20px;
    margin-top: 16px;
    border-top: 1px solid rgba(var(--v-theme-on-surface), 0.2);
    border-left: 0;
  }
}
</style>
