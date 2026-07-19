import { useAppMutation } from '@/shared/api/useAppMutation'
import { post } from '@/shared/api/http'
import type {
  ImportExecuteResponse,
  ImportSourceType,
} from '@/features/system/import/types/importApiTypes'

type Payload = {
  targetCode: string
  sourceType: ImportSourceType
  file?: File | null
}

export const useExecuteImportMutation = () => {
  return useAppMutation({
    mutationFn: async ({ targetCode, sourceType, file }: Payload) => {
      if (sourceType === 'UPLOAD') {
        if (!file) {
          throw new Error('CSVファイルを選択してください。')
        }

        const formData = new FormData()
        formData.append('targetCode', targetCode)
        formData.append('file', file)

        return await post<ImportExecuteResponse, FormData>(
          '/api/system/import/execute',
          formData,
        )
      }

      const formData = new FormData()
      formData.append('targetCode', targetCode)

      return await post<ImportExecuteResponse, FormData>(
        '/api/system/import/execute-defined',
        formData,
      )
    },
  })
}