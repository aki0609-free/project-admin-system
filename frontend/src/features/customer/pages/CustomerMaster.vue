<script setup lang="ts">
import CardLayout from '@/shared/components/layout/card_layout/CardLayout.vue'
import SimpleTable from '@/shared/components/table/simple_table/SimpleTable.vue'
import GenericToolbar from '@/shared/components/toolbar/GenericToolbar.vue'
import CustomerFormDialog from '../components/CustomerFormDialog.vue'
import EnvelopePrintDialog from '../components/EnvelopePrintDialog.vue'
import { useCustomerMasterPage } from '../composables/useCustomerMasterPage'
import PdfPreviewDialog from '@/shared/components/pdf/PdfPreviewDialog.vue'

const {
  columns,
  customersQuery,
  items,
  filterRules,
  toolbarItems,
  editDialog,
  envelopePrint,
} = useCustomerMasterPage()
</script>

<template>
  <CardLayout title="顧客マスター" subtitle="顧客情報一覧">
    <div class="d-flex flex-column ga-4">
      <GenericToolbar :items="toolbarItems" />

      <v-alert
        v-if="customersQuery.isError.value"
        type="error"
        variant="tonal"
      >
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

      <CustomerFormDialog
        v-model="editDialog.dialog.value"
        :customer="editDialog.editingCustomer.value"
        :sites="editDialog.editingSites.value"
        :employees="editDialog.editingEmployees.value"
        :is-create-mode="editDialog.isCreateMode.value"
        @save="editDialog.save"
        @delete="editDialog.remove"
      />

      <EnvelopePrintDialog
        v-model="envelopePrint.envelopeDialog.value"
        :customers="envelopePrint.envelopeCustomerOptions.value"
        @print="envelopePrint.handleEnvelopePrint"
      />
    </div>
  </CardLayout>

  <PdfPreviewDialog
    v-model="envelopePrint.pdfPreviewDialog.value"
    :title="envelopePrint.pdfPreviewTitle.value"
    :pdf-url="envelopePrint.pdfPreviewUrl.value"
    :pdf-file-key="envelopePrint.pdfFileKey.value"
    :pdf-file-name="envelopePrint.pdfFileName.value"
    :storage-type="envelopePrint.pdfStorageType.value ?? 'LOCAL'"
  />
</template>