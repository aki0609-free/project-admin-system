import { computed } from "vue"
import { useSimpleTablePaginationStore } from "../store/paging/useSimpleTablePaginationStore"
import { SimpleTableEditableRow } from "../types/item/types"

export function useSimpleTablePagination<T extends SimpleTableEditableRow>(tableKey: string) {
    const store = useSimpleTablePaginationStore()

    store.init(tableKey)

    const pagination = computed(() => store.tables[tableKey]!)

    const page = computed({
        get: () => pagination.value.page,
        set: (val: number) => store.setPage(tableKey, val)
    })

    const itemsPerPage = computed({
        get: () => pagination.value.itemsPerPage,
        set: (val: number) => store.setItemsPerPage(tableKey, val)
    })

    return {
        page,
        itemsPerPage,
        setPage: (p: number) => store.setPage(tableKey, p),
        setItemsPerPage: (s: number) => store.setItemsPerPage(tableKey, s),
    }

}