import { computed, reactive, type Ref } from 'vue'
import type { ApplicationMediaPivotRow, ApplicationMediaPivotRowBase, FilterState } from '@/features/application/types/applicationMediaTypes'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { formatYearMonth } from '@/shared/utils/DateUtils'

export const useApplicationMediaPivot = (
  medias: Ref<ApplicationMediaLocalItem[]>,
  markDirty: () => void,
) => {
  const filter = reactive<FilterState>({
    mediaYearMonth: '',
    mediaName: '',
  })

  const draftRows = reactive<string[]>([])
  const draftYearMonths = reactive<string[]>([])

  const filteredMedias = computed(() => {
    return medias.value.filter(item => {
      if (item.isDeleted) return false

      const matchYearMonth =
        !filter.mediaYearMonth || item.mediaYearMonth === filter.mediaYearMonth

      const matchMediaName =
        !filter.mediaName || item.mediaName.includes(filter.mediaName)

      return matchYearMonth && matchMediaName
    })
  })

  const mediaNames = computed(() => {
    const actual = filteredMedias.value
      .map(item => item.mediaName)
      .filter((v): v is string => !!v)

    return Array.from(new Set([...actual, ...draftRows])).sort()
  })

  const yearMonths = computed(() => {
    const actual = filteredMedias.value
      .map(item => item.mediaYearMonth)
      .filter((v): v is string => !!v)

    return Array.from(new Set([...actual, ...draftYearMonths])).sort((a, b) =>
      a.localeCompare(b),
    )
  })

  const allYearMonthOptions = computed(() => {
    const actual = medias.value
      .filter(item => !item.isDeleted)
      .map(item => item.mediaYearMonth)
      .filter((v): v is string => !!v)

    return Array.from(new Set([...actual, ...draftYearMonths]))
      .sort((a, b) => a.localeCompare(b))
      .map(value => ({
        label: formatYearMonth(value),
        value,
      }))
  })

  const pivotTableData = computed<ApplicationMediaPivotRow[]>(() => {
    const temp: Record<string, ApplicationMediaPivotRowBase> = {}

    for (const mediaName of mediaNames.value) {
      temp[mediaName] = { mediaName }

      for (const yearMonth of yearMonths.value) {
        temp[mediaName][`${yearMonth}_mediaArea`] = ''
        temp[mediaName][`${yearMonth}_mediaSlots`] = null
        temp[mediaName][`${yearMonth}_cost`] = null
        temp[mediaName][`${yearMonth}_hires`] = null
        temp[mediaName][`${yearMonth}_unitPrice`] = null
        temp[mediaName][`${yearMonth}_recordId`] = null
      }
    }

    for (const item of filteredMedias.value) {
      const mediaName = item.mediaName
      const yearMonth = item.mediaYearMonth

      if (!mediaName || !yearMonth) continue

      if (!temp[mediaName]) {
        temp[mediaName] = { mediaName }
      }

      temp[mediaName][`${yearMonth}_mediaArea`] = item.mediaArea ?? ''
      temp[mediaName][`${yearMonth}_mediaSlots`] = item.mediaSlots ?? null
      temp[mediaName][`${yearMonth}_cost`] = item.cost ?? null
      temp[mediaName][`${yearMonth}_hires`] = item.hires ?? null
      temp[mediaName][`${yearMonth}_unitPrice`] = item.unitPrice ?? null
      temp[mediaName][`${yearMonth}_recordId`] = item.id
    }

    return Object.values(temp).map((row, index) => ({
      ...row,
      id: index + 1,
    }))
  })

  function parsePivotField(field: string) {
    const match = field.match(
      /^(\d{4}-\d{2})_(mediaArea|mediaSlots|cost|hires|unitPrice)$/,
    )
    if (!match) return null

    return {
      mediaYearMonth: match[1],
      targetField: match[2] as
        | 'mediaArea'
        | 'mediaSlots'
        | 'cost'
        | 'hires'
        | 'unitPrice',
    }
  }

  function recalculateUnitPrice(record: ApplicationMediaLocalItem) {
    if ((record.hires ?? 0) > 0 && record.cost != null) {
      record.unitPrice = Math.round(record.cost / (record.hires ?? 1))
    } else {
      record.unitPrice = null
    }
  }

  function markUpdatedIfNeeded(target: ApplicationMediaLocalItem) {
    if (!target.isCreated) {
      target.isUpdated = true
    }
  }

  function handleCellUpdate(payload: {
    id: number
    field: keyof ApplicationMediaPivotRow
    value: unknown
  }) {
    const row = pivotTableData.value.find(item => item.id === payload.id)
    if (!row) return

    const parsed = parsePivotField(String(payload.field))
    if (!parsed) return

    const mediaName = row.mediaName
    const mediaYearMonth = parsed.mediaYearMonth

    let target = medias.value.find(
      item =>
        !item.isDeleted &&
        item.mediaName === mediaName &&
        item.mediaYearMonth === mediaYearMonth,
    )

    if (!target) {
      target = {
        id: Date.now(),
        mediaName,
        mediaArea: null,
        mediaSlots: null,
        mediaYearMonth: mediaYearMonth as string,
        cost: null,
        hires: null,
        unitPrice: null,
        isCreated: true,
        isUpdated: false,
        isDeleted: false,
      }
      medias.value.push(target)
    }

    if (parsed.targetField === 'mediaArea') {
      target.mediaArea = payload.value == null ? '' : String(payload.value)
      markUpdatedIfNeeded(target)
    }

    if (parsed.targetField === 'mediaSlots') {
      target.mediaSlots =
        payload.value === '' || payload.value == null
          ? null
          : Number(payload.value)
      markUpdatedIfNeeded(target)
    }

    if (parsed.targetField === 'cost') {
      target.cost =
        payload.value === '' || payload.value == null
          ? null
          : Number(payload.value)
      recalculateUnitPrice(target)
      markUpdatedIfNeeded(target)
    }

    markDirty()
  }

  function addDraftMedia(name: string) {
    const value = name.trim()
    if (!value) return

    if (!draftRows.includes(value)) {
      draftRows.push(value)
      markDirty()
    }
  }

  function addDraftYearMonth(value: string) {
    const yearMonth = value.trim()
    if (!/^\d{4}-\d{2}$/.test(yearMonth)) return

    if (!draftYearMonths.includes(yearMonth)) {
      draftYearMonths.push(yearMonth)
      markDirty()
    }
  }

  function deleteMedia(mediaName: string) {
    const next: ApplicationMediaLocalItem[] = []

    for (const item of medias.value) {
      if (item.mediaName !== mediaName) {
        next.push(item)
        continue
      }

      if (item.isCreated) {
        continue
      }

      next.push({
        ...item,
        isDeleted: true,
        isUpdated: false,
      })
    }

    medias.value = next

    const draftIndex = draftRows.indexOf(mediaName)
    if (draftIndex >= 0) draftRows.splice(draftIndex, 1)

    markDirty()
  }

  function deleteYearMonth(yearMonth: string) {
    const next: ApplicationMediaLocalItem[] = []

    for (const item of medias.value) {
      if (item.mediaYearMonth !== yearMonth) {
        next.push(item)
        continue
      }

      if (item.isCreated) {
        continue
      }

      next.push({
        ...item,
        isDeleted: true,
        isUpdated: false,
      })
    }

    medias.value = next

    const draftIndex = draftYearMonths.indexOf(yearMonth)
    if (draftIndex >= 0) draftYearMonths.splice(draftIndex, 1)

    markDirty()
  }

  return {
    filter,
    draftRows,
    draftYearMonths,
    filteredMedias,
    mediaNames,
    yearMonths,
    allYearMonthOptions,
    pivotTableData,
    handleCellUpdate,
    addDraftMedia,
    addDraftYearMonth,
    deleteMedia,
    deleteYearMonth,
  }
}