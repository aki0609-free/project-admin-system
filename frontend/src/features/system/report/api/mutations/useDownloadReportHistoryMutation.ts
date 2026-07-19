import { getBlob } from '@/shared/api/http'
import { useAppMutation } from '@/shared/api/useAppMutation'

const openBlob = (
  blob: Blob,
  fallbackFileName: string,
) => {
  const url = URL.createObjectURL(blob)

  const opened = window.open(
    url,
    '_blank',
    'noopener,noreferrer',
  )

  if (!opened) {
    const link = document.createElement('a')
    link.href = url
    link.download = fallbackFileName
    link.click()
  }

  setTimeout(() => {
    URL.revokeObjectURL(url)
  }, 60_000)
}

export const useDownloadReportHistoryMutation = () => {
  return useAppMutation({
    mutationFn: async (id: number): Promise<Blob> => {
      return await getBlob(
        `/api/system/report-histories/${id}/download`,
      )
    },

    onSuccess: async (
      blob: Blob,
      id: number,
    ) => {
      openBlob(
        blob,
        `report-history-${id}.pdf`,
      )
    },
  })
}