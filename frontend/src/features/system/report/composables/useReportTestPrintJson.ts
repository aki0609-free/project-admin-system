import { computed, ref, watch, type Ref } from 'vue'
import { useReportTestParamTemplatesQuery } from '@/features/system/report/api/queries/useReportTestParamTemplatesQuery'
import { useCreateReportTestParamTemplateMutation } from '@/features/system/report/api/mutations/useCreateReportTestParamTemplateMutation'

type TestPrintJson = {
  reportCode?: string
  params?: Record<string, unknown>
  [key: string]: unknown
}

const createSampleJson = (reportCode: string) => ({
  reportCode,
  params: {
    message: 'シンプル帳票テスト',
    target_name: 'AKIさん',
  },
})

export const useReportTestPrintJson = (
  selectedReportCode: Ref<string | null>,
) => {
  const paramsText = ref(
    JSON.stringify(createSampleJson('RPT_TEST_SIMPLE'), null, 2),
  )

  const templateName = ref('テストJSON')

  const templatesQuery = useReportTestParamTemplatesQuery(selectedReportCode)
  const createMutation = useCreateReportTestParamTemplateMutation()

  const jsonErrorMessage = computed(() => {
    try {
      JSON.parse(paramsText.value || '{}')
      return ''
    } catch (e) {
      return e instanceof Error ? e.message : 'JSON形式が不正です。'
    }
  })

  const hasJsonError = computed(() => jsonErrorMessage.value !== '')

  const parsedJson = computed<TestPrintJson>(() => {
    if (hasJsonError.value) return {}
    return JSON.parse(paramsText.value || '{}')
  })

  const apiParams = computed<Record<string, unknown>>(() => {
    const value = parsedJson.value

    if (
      value.params &&
      typeof value.params === 'object' &&
      !Array.isArray(value.params)
    ) {
      return value.params as Record<string, unknown>
    }

    return value as Record<string, unknown>
  })

  const formatJson = () => {
    if (hasJsonError.value) {
      alert('JSON形式が不正なため整形できません。')
      return
    }

    paramsText.value = JSON.stringify(parsedJson.value, null, 2)
  }

  const generateSampleJson = () => {
    const reportCode = selectedReportCode.value ?? 'RPT_TEST_SIMPLE'
    paramsText.value = JSON.stringify(createSampleJson(reportCode), null, 2)
  }

  const saveCurrentJson = async () => {
    const reportCode = selectedReportCode.value

    if (!reportCode) {
      alert('帳票を選択してください。')
      return
    }

    if (hasJsonError.value) {
      alert('JSON形式が不正です。')
      return
    }

    await createMutation.mutateAsync({
      reportCode,
      templateName: templateName.value || 'テストJSON',
      jsonText: JSON.stringify(parsedJson.value, null, 2),
      defaultFlag: false,
      activeFlag: true,
    })

    alert('テストJSONを保存しました。')
  }

  const applyTemplate = (jsonText: string) => {
    paramsText.value = jsonText
  }

  watch(
    () => selectedReportCode.value,
    (reportCode) => {
      if (!reportCode) return

      const defaultTemplate = templatesQuery.templates.value.find(
        (x) => x.defaultFlag,
      )

      if (defaultTemplate) {
        paramsText.value = defaultTemplate.jsonText
        return
      }

      paramsText.value = JSON.stringify(createSampleJson(reportCode), null, 2)
    },
  )

  return {
    paramsText,
    templateName,
    hasJsonError,
    jsonErrorMessage,
    parsedJson,
    apiParams,
    templates: templatesQuery.templates,
    isTemplatesLoading: templatesQuery.isLoading,
    isSavingTemplate: createMutation.isPending,
    formatJson,
    saveCurrentJson,
    applyTemplate,
    generateSampleJson,
  }
}