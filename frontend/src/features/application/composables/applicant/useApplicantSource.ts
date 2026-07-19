import { computed } from 'vue'
import { useApplicantsQuery } from '@/features/application/api/useApplicantsQuery'
import { useCreateApplicantMutation } from '@/features/application/api/useCreateApplicantMutation'
import { useUpdateApplicantMutation } from '@/features/application/api/useUpdateApplicantMutation'
import { useDeleteApplicantMutation } from '@/features/application/api/useDeleteApplicantMutation'
import { toApplicantCreateRequest, toApplicantUpdateRequest } from '@/features/application/mapper/applicantRequestMapper'
import { toApplicantRow } from '@/features/application/mapper/applicantResponseMapper'
import type { ApplicantPersistedRow } from '@/features/application/types/applicantTypes'

export const useApplicantSource = () => {
  const applicantsQuery = useApplicantsQuery()
  const createMutation = useCreateApplicantMutation()
  const updateMutation = useUpdateApplicantMutation()
  const deleteMutation = useDeleteApplicantMutation()

  const applicants = computed(() =>
    (applicantsQuery.applicants.value ?? []).map(toApplicantRow),
  )

  const createApplicant = async (payload: ApplicantPersistedRow) => {
    await createMutation.mutateAsync(toApplicantCreateRequest(payload))
  }

  const updateApplicant = async (payload: ApplicantPersistedRow) => {
    await updateMutation.mutateAsync(toApplicantUpdateRequest(payload))
  }

  const deleteApplicant = async (id: number) => {
    await deleteMutation.mutateAsync(id)
  }

  const isSaving = computed(
    () =>
      createMutation.isPending.value ||
      updateMutation.isPending.value ||
      deleteMutation.isPending.value,
  )

  return {
    applicantsQuery,
    applicants,
    createApplicant,
    updateApplicant,
    deleteApplicant,
    isSaving,
  }
}