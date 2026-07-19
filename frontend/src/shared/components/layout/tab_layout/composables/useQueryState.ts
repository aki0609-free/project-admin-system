import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export const useQueryState = <T extends Record<string, any>>(
  defaults: T
) => {
  const route = useRoute()
  const router = useRouter()

  const state = computed<T>({
    get() {
      const query = route.query
      const result: any = {}

      for (const key in defaults) {
        const value = query[key]

        if (value === undefined) {
          result[key] = defaults[key]
        } else {
          result[key] = value
        }
      }

      return result
    },
    set(newState: T) {
      router.replace({
        query: {
          ...route.query,
          ...newState,
        },
      })
    },
  })

  return { state }
}