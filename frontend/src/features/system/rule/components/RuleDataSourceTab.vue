<script setup lang="ts">
import { computed, ref } from 'vue'

import type {
  RuleColumnMappingForm,
  RuleDataSourceForm,
  RuleMasterForm,
} from '@/features/system/rule/types/ruleFormTypes'
import type { RuleDataType } from '@/features/system/rule/types/ruleApiTypes'

import RuleDataSourceList from './rule_datasource/RuleDataSourceList.vue'
import RuleDataSourceEditor from './rule_datasource/RuleDataSourceEditor.vue'
import RuleColumnList from './rule_datasource/RuleColumnList.vue'
import RuleColumnEditor from './rule_datasource/RuleColumnEditor.vue'
import { useRuleDataSourceCatalogsQuery } from '@/features/system/rule/api/useRuleDataSourceCatalogsQuery'

const catalogsQuery = useRuleDataSourceCatalogsQuery()

const props = defineProps<{
  form: RuleMasterForm
}>()

const selectedDataSource = ref<RuleDataSourceForm | null>(null)
const selectedColumn = ref<RuleColumnMappingForm | null>(null)
const selectedCatalog = computed(() =>
  catalogsQuery.catalogs.value.find(
    catalog =>
      catalog.sourceCode === selectedDataSource.value?.catalogCode,
  ) ?? null,
)

const selectDataSource = (source: RuleDataSourceForm | null) => {
  selectedDataSource.value = source
  selectedColumn.value = null
}

const selectColumn = (column: RuleColumnMappingForm | null) => {
  selectedColumn.value = column
}

const applyCatalogColumn = (
  columnName: string,
  dataType: RuleDataType,
) => {
  if (!selectedColumn.value) return

  selectedColumn.value.columnName = columnName
  selectedColumn.value.dataType = dataType
}
</script>

<template>
  <div class="datasource-tab">
    <section class="datasource-list-pane">
      <RuleDataSourceList
        :form="props.form"
        :selected-data-source="selectedDataSource"
        @select="selectDataSource"
      />
    </section>

    <section class="datasource-work-pane">
      <RuleDataSourceEditor
        :data-source="selectedDataSource as RuleDataSourceForm"
        :catalogs="catalogsQuery.catalogs.value"
      />

      <div class="column-work-area">
        <RuleColumnList
          :data-source="selectedDataSource"
          :selected-column="selectedColumn"
          @select="selectColumn"
        />

        <RuleColumnEditor
          :column="selectedColumn as RuleColumnMappingForm"
          :available-columns="selectedCatalog?.columns ?? []"
          @select-column="applyCatalogColumn"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.datasource-tab {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  min-height: 620px;
}

.datasource-list-pane,
.datasource-work-pane {
  min-width: 0;
}

.datasource-work-pane {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
}

.column-work-area {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 16px;
  min-height: 360px;
}

@media (max-width: 1280px) {
  .datasource-tab,
  .column-work-area {
    grid-template-columns: 1fr;
  }
}
</style>
