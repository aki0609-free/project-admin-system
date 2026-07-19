import { computed, reactive, type Ref } from 'vue'
import type { ToolbarItem } from '@/shared/components/toolbar/types/types'
import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import { formatYearMonth } from '@/shared/utils/DateUtils'

export const useApplicationMediaChartFilter = (
  medias: Ref<ApplicationMediaLocalItem[]>,
) => {
  const filter = reactive({
    mediaYearMonth: '',
    mediaName: '',
    mediaArea: '',
  })

  const visibleMedias = computed(() =>
    medias.value.filter(item => !item.isDeleted),
  )

  const yearMonthOptions = computed(() =>
    Array.from(
      new Set(
        visibleMedias.value
          .map(item => item.mediaYearMonth)
          .filter((v): v is string => !!v),
      ),
    )
      .sort()
      .map(value => ({
        label: formatYearMonth(value),
        value,
      })),
  )

  const mediaNameOptions = computed(() =>
    Array.from(
      new Set(
        visibleMedias.value
          .map(item => item.mediaName)
          .filter((v): v is string => !!v),
      ),
    )
      .sort()
      .map(value => ({
        label: value,
        value,
      })),
  )

  const mediaAreaOptions = computed(() =>
    Array.from(
      new Set(
        visibleMedias.value
          .map(item => item.mediaArea)
          .filter((v): v is string => !!v),
      ),
    )
      .sort()
      .map(value => ({
        label: value,
        value,
      })),
  )

  const toolbarItems = computed<ToolbarItem[]>(() => [
    {
      type: 'dropdown',
      label: '掲載年月',
      options: [{ label: 'すべて', value: '' }, ...yearMonthOptions.value],
      onSelect: (value: string) => {
        filter.mediaYearMonth = value
      },
    },
    {
      type: 'dropdown',
      label: '媒体名',
      options: [{ label: 'すべて', value: '' }, ...mediaNameOptions.value],
      onSelect: (value: string) => {
        filter.mediaName = value
      },
    },
    {
      type: 'dropdown',
      label: '掲載地域',
      options: [{ label: 'すべて', value: '' }, ...mediaAreaOptions.value],
      onSelect: (value: string) => {
        filter.mediaArea = value
      },
    },
  ])

  const filteredMedias = computed(() =>
    visibleMedias.value.filter(item => {
      const matchYearMonth =
        !filter.mediaYearMonth || item.mediaYearMonth === filter.mediaYearMonth
      const matchMediaName =
        !filter.mediaName || item.mediaName === filter.mediaName
      const matchMediaArea =
        !filter.mediaArea || item.mediaArea === filter.mediaArea

      return matchYearMonth && matchMediaName && matchMediaArea
    }),
  )

  return {
    filter,
    visibleMedias,
    yearMonthOptions,
    mediaNameOptions,
    mediaAreaOptions,
    toolbarItems,
    filteredMedias,
  }
}