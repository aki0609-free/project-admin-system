import { ref } from 'vue'

export const useApplicationMediaTableDialog = () => {
  const createMediaDialog = ref(false)
  const createYearMonthDialog = ref(false)
  const deleteMediaDialog = ref(false)
  const deleteYearMonthDialog = ref(false)

  const newMediaName = ref('')
  const newYear = ref<string | number>('')
  const newMonth = ref<string | number>('')

  const deleteMediaName = ref('')
  const deleteYearMonthValue = ref('')

  const openMediaDialog = () => {
    createMediaDialog.value = true
  }

  const closeMediaDialog = () => {
    createMediaDialog.value = false
  }

  const openYearMonthDialog = () => {
    createYearMonthDialog.value = true
  }

  const closeYearMonthDialog = () => {
    createYearMonthDialog.value = false
  }

  const openDeleteMediaDialog = () => {
    deleteMediaDialog.value = true
  }

  const closeDeleteMediaDialog = () => {
    deleteMediaDialog.value = false
  }

  const openDeleteYearMonthDialog = () => {
    deleteYearMonthDialog.value = true
  }

  const closeDeleteYearMonthDialog = () => {
    deleteYearMonthDialog.value = false
  }

  return {
    createMediaDialog,
    createYearMonthDialog,
    deleteMediaDialog,
    deleteYearMonthDialog,
    newMediaName,
    newYear,
    newMonth,
    deleteMediaName,
    deleteYearMonthValue,
    openMediaDialog,
    closeMediaDialog,
    openYearMonthDialog,
    closeYearMonthDialog,
    openDeleteMediaDialog,
    closeDeleteMediaDialog,
    openDeleteYearMonthDialog,
    closeDeleteYearMonthDialog,
  }
}