import { computed, Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export const useRouteTab = (
  defaultTab: string,
  externalModel?: Ref<string>,
  validTabs?: string[]
) => {
  // Story用（Routerなし）
  if (externalModel) {
    return { activeTab: externalModel }
  }

  const route = useRoute()
  const router = useRouter()

  const activeTab = computed<string>({
    get() {
      const tab = route.query.tab as string | undefined

      if (!tab) return defaultTab

      if (validTabs && !validTabs.includes(tab)) {
        return defaultTab
      }

      return tab
    },
    set(value: string) {
      router.replace({
        query: {
          ...route.query,
          tab: value,
        },
      })
    },
  })

  return { activeTab }
}