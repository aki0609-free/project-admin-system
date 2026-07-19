import type { ApplicationMediaLocalItem } from '@/features/application/types/applicationMediaTypes'
import type {
  ApplicationMediaBulkCreateItem,
  ApplicationMediaBulkSaveRequest,
  ApplicationMediaBulkUpdateItem,
} from '@/features/application/types/types'

function toNullableString(value: string | null | undefined): string | null {
  if (value == null || value === '') return null
  return value
}

export function toApplicationMediaBulkCreateItem(
  item: ApplicationMediaLocalItem,
): ApplicationMediaBulkCreateItem {
  return {
    mediaName: item.mediaName,
    mediaArea: toNullableString(item.mediaArea),
    mediaSlots: item.mediaSlots ?? null,
    mediaYearMonth: item.mediaYearMonth ?? null,
    cost: item.cost ?? null,
  }
}

export function toApplicationMediaBulkUpdateItem(
  item: ApplicationMediaLocalItem,
): ApplicationMediaBulkUpdateItem {
  return {
    id: item.id,
    mediaName: item.mediaName,
    mediaArea: toNullableString(item.mediaArea),
    mediaSlots: item.mediaSlots ?? null,
    mediaYearMonth: item.mediaYearMonth ?? null,
    cost: item.cost ?? null,
  }
}

export function toApplicationMediaBulkSaveRequest(
  medias: ApplicationMediaLocalItem[],
): ApplicationMediaBulkSaveRequest {
  const created = medias
    .filter(item => item.isCreated && !item.isDeleted)
    .map(toApplicationMediaBulkCreateItem)

  const updated = medias
    .filter(item => !item.isCreated && item.isUpdated && !item.isDeleted)
    .map(toApplicationMediaBulkUpdateItem)

  const deletedIds = medias
    .filter(item => !item.isCreated && item.isDeleted)
    .map(item => item.id)

  return {
    created,
    updated,
    deletedIds,
  }
}