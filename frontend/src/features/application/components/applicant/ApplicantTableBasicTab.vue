<script setup lang="ts">
import { computed } from 'vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import { createSimpleTableFilterRules } from '@/shared/components/table/simple_table/utils/createSimpleTableFilterRules'
import { useApplicantBasicColumns } from '@/features/application/composables/applicant/useApplicantBasicColumns'
import type { ApplicantRow } from '@/features/application/types/applicantTypes'

const props = defineProps<{
  applicants: ApplicantRow[]
}>()

const emit = defineEmits<{
  (e: 'row-click', row: ApplicantRow): void
}>();

const { columns } = useApplicantBasicColumns()

const filterRules = computed(() =>
  createSimpleTableFilterRules<ApplicantRow>(columns.value),
)
</script>

<template>
  <SimpleTable
    tableKey="applicant-basic-table"
    itemKey="id"
    :items="props.applicants"
    :columns="columns"
    :filterRules="filterRules"
    :enableRowClick="true"
    @row-click="(row) => emit('row-click', row)"
  />
</template>