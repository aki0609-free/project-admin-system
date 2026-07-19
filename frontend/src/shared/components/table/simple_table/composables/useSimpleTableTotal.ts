import { computed, ComputedRef, Ref } from "vue";

export function useSimpleTableTotal<T>(
    filteredItems: Ref<T[]> | ComputedRef<T[]>,
) {
    const createTotal = (key: keyof T | 'count') =>
        computed(() => {
            if (key === 'count') {
                return filteredItems.value.length
            }

            return filteredItems.value.reduce((sum: number, item: T) => {
                const value = Number(item[key] ?? 0)
                return sum + (isNaN(value) ? 0 : value)
            }, 0)
        })
    
    return {
        createTotal
    }
}