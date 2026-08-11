<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { payrollItemTransactionApi } from '../api/payrollItemTransactionApi'
import type {
  EmployeePayrollItemTransaction,
  EmployeePayrollItemTransactionRequest,
} from '../types/payrollItemTransactionTypes'

const props = defineProps<{
  employeeId: number
  targetCode: string
  targetName: string
  quantityUnit?: string | null
}>()

const formatLocalDate = (date: Date) => [
  date.getFullYear(),
  String(date.getMonth() + 1).padStart(2, '0'),
  String(date.getDate()).padStart(2, '0'),
].join('-')
const today = formatLocalDate(new Date())
const selectedMonth = ref(today.slice(0, 7))
const transactions = ref<EmployeePayrollItemTransaction[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const editingId = ref<number | null>(null)

const form = reactive<EmployeePayrollItemTransactionRequest>({
  targetCode: props.targetCode,
  targetMonth: selectedMonth.value,
  transactionDate: today,
  amount: 0,
  quantity: null,
  status: 'CONFIRMED',
  sourceReference: null,
  note: null,
})

const showQuantity = computed(() => props.quantityUnit === 'DAYS')
const defaultTransactionDate = () => selectedMonth.value === today.slice(0, 7)
  ? today
  : `${selectedMonth.value}-01`

const reset = () => {
  editingId.value = null
  Object.assign(form, {
    targetCode: props.targetCode,
    targetMonth: selectedMonth.value,
    transactionDate: defaultTransactionDate(),
    amount: 0,
    quantity: null,
    status: 'CONFIRMED',
    sourceReference: null,
    note: null,
  })
}

const load = async () => {
  if (!props.employeeId || !props.targetCode || !selectedMonth.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    transactions.value = await payrollItemTransactionApi.findAll(
      props.employeeId,
      props.targetCode,
      selectedMonth.value,
    )
  } catch {
    errorMessage.value = '控除明細を取得できませんでした。'
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (form.amount <= 0) {
    errorMessage.value = '金額は1円以上で入力してください。'
    return
  }
  saving.value = true
  errorMessage.value = ''
  const request: EmployeePayrollItemTransactionRequest = {
    ...form,
    targetCode: props.targetCode,
    targetMonth: selectedMonth.value,
    quantity: showQuantity.value ? form.quantity : null,
    sourceReference: form.sourceReference?.trim() || null,
    note: form.note?.trim() || null,
  }
  try {
    if (editingId.value) {
      await payrollItemTransactionApi.update(
        props.employeeId, editingId.value, request,
      )
    } else {
      await payrollItemTransactionApi.create(props.employeeId, request)
    }
    reset()
    await load()
  } catch {
    errorMessage.value = '控除明細を保存できませんでした。入力内容と重複を確認してください。'
  } finally {
    saving.value = false
  }
}

const edit = (item: EmployeePayrollItemTransaction) => {
  editingId.value = item.id
  Object.assign(form, {
    targetCode: item.targetCode,
    targetMonth: item.targetMonth,
    transactionDate: item.transactionDate,
    amount: item.amount,
    quantity: item.quantity,
    status: item.status,
    sourceReference: item.sourceReference,
    note: item.note,
  })
}

const remove = async (item: EmployeePayrollItemTransaction) => {
  if (!window.confirm(`${item.targetName} ${item.amount.toLocaleString()}円を削除しますか？`)) return
  try {
    await payrollItemTransactionApi.remove(props.employeeId, item.id)
    if (editingId.value === item.id) reset()
    await load()
  } catch {
    errorMessage.value = '控除明細を削除できませんでした。'
  }
}

watch(
  [() => props.employeeId, () => props.targetCode, selectedMonth],
  () => {
    reset()
    void load()
  },
  { immediate: true },
)
</script>

<template>
  <section class="transaction-panel">
    <div class="transaction-heading">
      <div>
        <h3>明細・月次控除</h3>
        <p>明細到着時、または月次一括徴収時に登録します。確定した明細だけが月次締めへ反映されます。</p>
      </div>
      <v-text-field v-model="selectedMonth" label="対象月" type="month" variant="outlined" hide-details />
    </div>

    <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-4">{{ errorMessage }}</v-alert>

    <div class="transaction-form">
      <v-text-field v-model="form.transactionDate" label="明細日" type="date" variant="outlined" />
      <v-text-field v-model.number="form.amount" label="控除金額" type="number" min="1" suffix="円" variant="outlined" />
      <v-text-field
        v-if="showQuantity"
        v-model.number="form.quantity"
        label="対象日数"
        type="number"
        min="0"
        suffix="日"
        variant="outlined"
      />
      <v-select
        v-model="form.status"
        label="状態"
        :items="[{ title: '確定', value: 'CONFIRMED' }, { title: '下書き', value: 'DRAFT' }]"
        variant="outlined"
      />
      <v-text-field v-model="form.sourceReference" label="明細番号（任意）" variant="outlined" />
      <v-text-field v-model="form.note" label="備考" variant="outlined" />
    </div>
    <div class="transaction-actions">
      <v-btn v-if="editingId" variant="text" @click="reset">編集取消</v-btn>
      <v-btn color="primary" :loading="saving" @click="save">
        {{ editingId ? '明細を更新' : '明細を追加' }}
      </v-btn>
    </div>

    <v-table density="compact" class="mt-4">
      <thead>
        <tr><th>明細日</th><th>金額</th><th>状態</th><th>明細番号</th><th>備考</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-if="loading"><td colspan="6">読み込み中...</td></tr>
        <tr v-else-if="transactions.length === 0"><td colspan="6">登録された明細はありません。</td></tr>
        <tr v-for="item in transactions" :key="item.id">
          <td>{{ item.transactionDate }}</td>
          <td>{{ item.amount.toLocaleString() }}円</td>
          <td>{{ item.status === 'CONFIRMED' ? '確定' : '下書き' }}</td>
          <td>{{ item.sourceReference || '-' }}</td>
          <td>{{ item.note || '-' }}</td>
          <td class="row-actions">
            <v-btn size="small" variant="text" @click="edit(item)">編集</v-btn>
            <v-btn size="small" variant="text" color="error" @click="remove(item)">削除</v-btn>
          </td>
        </tr>
      </tbody>
    </v-table>
  </section>
</template>

<style scoped>
.transaction-panel { margin-top: 20px; border-top: 1px solid rgba(0, 0, 0, 0.12); padding-top: 20px; }
.transaction-heading { display: grid; grid-template-columns: 1fr 180px; gap: 16px; align-items: start; }
.transaction-heading h3 { margin: 0 0 4px; }
.transaction-heading p { margin: 0; color: rgba(0, 0, 0, 0.65); }
.transaction-form { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
.transaction-actions, .row-actions { display: flex; justify-content: flex-end; gap: 8px; }
@media (max-width: 900px) {
  .transaction-heading, .transaction-form { grid-template-columns: 1fr; }
}
</style>
