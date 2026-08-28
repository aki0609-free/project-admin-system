<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import { useApplicantMediaColumns } from '@/features/application/composables/applicant/useApplicantMediaColumns'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'

const props = defineProps<{
  applicants: ApplicantRow[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: ApplicantRow): void
}>();

const { columns } = useApplicantMediaColumns()

const filterRules = computed(() =>
  createSimpleTableFilterRules<ApplicantRow>(columns.value),
)
</script>

<template>
  <SimpleTable
    table-key="applicant-media-table"
    item-key="id"
    :items="props.applicants"
    :columns="columns"
    :filter-rules="filterRules"
    :enable-row-click="true"
    @row-click="(row) => emit('row-click', row)"
  />
</template>
