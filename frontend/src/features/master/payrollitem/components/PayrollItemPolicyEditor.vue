<script setup lang="ts">
import type {
  PayrollItemParameterDefinition,
  PayrollItemPolicy,
} from '../types/payrollItemPolicyTypes'

const policy = defineModel<PayrollItemPolicy>({ required: true })

defineProps<{
  canManage: boolean
}>()

const addDefinition = () => {
  const nextOrder = policy.value.parameterDefinitions.length * 10 + 10
  policy.value.parameterDefinitions.push({
    key: '',
    displayName: '',
    inputType: 'TEXT',
    required: false,
    defaultValue: null,
    options: [],
    ruleParameter: false,
    dailyDisplay: false,
    inputSourceOverride: false,
    ruleValueResolverKey: null,
    displayOrder: nextOrder,
  })
}

const removeDefinition = (index: number) => {
  policy.value.parameterDefinitions.splice(index, 1)
}

const addOption = (definition: PayrollItemParameterDefinition) => {
  definition.options.push({ label: '', value: '' })
}
</script>

<template>
  <v-card variant="outlined" class="mt-5 pa-4">
    <v-card-title class="px-0 pt-0">従業員・日報連携</v-card-title>
    <v-row>
      <v-col cols="12" md="6">
        <v-select
          v-model="policy.applicationScope"
          label="適用対象"
          :items="[
            { title: '全従業員へ自動適用', value: 'ALL_EMPLOYEES' },
            { title: '従業員ごとに設定', value: 'EMPLOYEE_ENROLLMENT' },
          ]"
          :readonly="!canManage"
          variant="outlined"
        />
      </v-col>
      <v-col cols="12" md="6">
        <v-select
          v-model="policy.inputSource"
          label="標準入力元"
          :items="[
            { title: '日報', value: 'DAILY_REPORT' },
            { title: '明細取引', value: 'TRANSACTION' },
          ]"
          :readonly="!canManage"
          variant="outlined"
        />
      </v-col>
      <v-col cols="12" md="3">
        <v-switch
          v-model="policy.balanceTracking"
          label="残高を管理する"
          :readonly="!canManage"
          color="primary"
        />
      </v-col>
      <v-col v-if="policy.balanceTracking" cols="12" md="3">
        <v-select
          v-model="policy.balanceUnit"
          label="残高単位"
          :items="[
            { title: '金額', value: 'AMOUNT' },
            { title: '日数', value: 'DAYS' },
            { title: '時間', value: 'HOURS' },
            { title: '回数', value: 'COUNT' },
          ]"
          :readonly="!canManage"
          variant="outlined"
        />
      </v-col>
      <v-col v-if="policy.balanceTracking" cols="12" md="3">
        <v-switch
          v-model="policy.carryForward"
          label="残高を繰り越す"
          :readonly="!canManage"
          color="primary"
        />
      </v-col>
      <v-col v-if="policy.balanceTracking" cols="12" md="3">
        <v-switch
          v-model="policy.advanceConsumption"
          label="残高超過を許可"
          :readonly="!canManage"
          color="primary"
        />
      </v-col>
    </v-row>

    <template v-if="policy.applicationScope === 'EMPLOYEE_ENROLLMENT'">
      <div class="d-flex align-center justify-space-between mt-3 mb-2">
        <strong>従業員別入力項目</strong>
        <v-btn v-if="canManage" size="small" variant="tonal" @click="addDefinition">
          入力項目を追加
        </v-btn>
      </div>

      <v-expansion-panels variant="accordion">
        <v-expansion-panel
          v-for="(definition, index) in policy.parameterDefinitions"
          :key="`${definition.key}-${index}`"
        >
          <v-expansion-panel-title>
            {{ definition.displayName || definition.key || `入力項目 ${index + 1}` }}
          </v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-row>
              <v-col cols="12" md="4">
                <v-text-field v-model="definition.key" label="パラメーターキー" :readonly="!canManage" />
              </v-col>
              <v-col cols="12" md="4">
                <v-text-field v-model="definition.displayName" label="表示名" :readonly="!canManage" />
              </v-col>
              <v-col cols="12" md="4">
                <v-select
                  v-model="definition.inputType"
                  label="入力型"
                  :items="['TEXT', 'NUMBER', 'SELECT', 'BOOLEAN', 'DATE']"
                  :readonly="!canManage"
                />
              </v-col>
              <v-col cols="12" md="4">
                <v-text-field v-model="definition.defaultValue" label="初期値" :readonly="!canManage" />
              </v-col>
              <v-col cols="12" md="2"><v-switch v-model="definition.required" label="必須" :readonly="!canManage" /></v-col>
              <v-col cols="12" md="2"><v-switch v-model="definition.ruleParameter" label="Ruleへ渡す" :readonly="!canManage" /></v-col>
              <v-col cols="12" md="2"><v-switch v-model="definition.dailyDisplay" label="日報に表示" :readonly="!canManage" /></v-col>
              <v-col cols="12" md="2"><v-switch v-model="definition.inputSourceOverride" label="入力元切替" :readonly="!canManage" /></v-col>
              <v-col cols="12" md="4">
                <v-text-field
                  v-model="definition.ruleValueResolverKey"
                  label="Rule値Resolverキー（任意）"
                  hint="外部マスターからRule値を解決する場合のみ指定"
                  persistent-hint
                  :readonly="!canManage"
                />
              </v-col>
            </v-row>

            <template v-if="definition.inputType === 'SELECT'">
              <div class="d-flex align-center justify-space-between mb-2">
                <span>選択肢</span>
                <v-btn v-if="canManage" size="x-small" @click="addOption(definition)">追加</v-btn>
              </div>
              <v-row v-for="(option, optionIndex) in definition.options" :key="optionIndex">
                <v-col cols="5"><v-text-field v-model="option.label" label="表示名" :readonly="!canManage" /></v-col>
                <v-col cols="5"><v-text-field v-model="option.value" label="値" :readonly="!canManage" /></v-col>
                <v-col cols="2"><v-btn v-if="canManage" color="error" variant="text" @click="definition.options.splice(optionIndex, 1)">削除</v-btn></v-col>
              </v-row>
            </template>

            <v-btn v-if="canManage" color="error" variant="text" @click="removeDefinition(index)">
              入力項目を削除
            </v-btn>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>
  </v-card>
</template>
