<script setup lang="ts">
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import ListDetailPageLayout from '@/shared/templates/list-detail/ListDetailPageTemplate.vue'
import CustomerFormDialog from '../components/CustomerFormDialog.vue'
import EnvelopePrintDialog from '../components/EnvelopePrintDialog.vue'
import { useCustomerMasterPage } from '../composables/useCustomerMasterPage'
import PdfPreviewDialog from '@/shared/components/pdf/PdfPreviewDialog.vue'

const { columns, customersQuery, items, filterRules, toolbarItems, editDialog, envelopePrint } =
  useCustomerMasterPage()
</script>

<template>
  <ListDetailPageLayout
    title="顧客管理"
    description="顧客情報、現場、担当者、請求単価を管理します。"
    :left-toolbar-items="toolbarItems.slice(0, 1)"
    :right-toolbar-items="toolbarItems.slice(1)"
  >
      <v-alert v-if="customersQuery.isError.value" type="error" variant="tonal">
        顧客一覧の取得に失敗しました。
      </v-alert>

      <SimpleTable
        table-key="customer-master"
        item-key="id"
        :items="items"
        :columns="columns"
        :filter-rules="filterRules"
        :enable-row-click="true"
        @row-click="editDialog.openEdit"
      />

    <template #dialogs>
      <CustomerFormDialog
          v-model="editDialog.dialog.value"
          :customer="editDialog.editingCustomer.value"
          :sites="editDialog.editingSites.value"
          :employees="editDialog.editingEmployees.value"
          :is-create-mode="editDialog.isCreateMode.value"
          :loading="editDialog.loading.value"
          :error-message="editDialog.errorMessage.value"
          @save="editDialog.save"
          @delete="editDialog.remove"
          @dismiss-error="editDialog.clearError"
        />

        <EnvelopePrintDialog
          v-model="envelopePrint.envelopeDialog.value"
          :customers="envelopePrint.envelopeCustomerOptions.value"
          @print="envelopePrint.handleEnvelopePrint"
        />

      <PdfPreviewDialog
        v-model="envelopePrint.pdfPreviewDialog.value"
        :title="envelopePrint.pdfPreviewTitle.value"
        :pdf-url="envelopePrint.pdfPreviewUrl.value"
        :pdf-file-key="envelopePrint.pdfFileKey.value"
        :pdf-file-name="envelopePrint.pdfFileName.value"
        :storage-type="envelopePrint.pdfStorageType.value ?? 'LOCAL'"
      />
    </template>
  </ListDetailPageLayout>
</template>
