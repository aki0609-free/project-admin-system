<script
  setup
  lang="ts"
  generic="T extends Record<string, any>"
>
import { ref, inject, computed } from 'vue'
import EditableFormCell from '../base/EditableFormCell.vue'
import { FormContextKey } from '../base/types/types'
import type { FormFieldDef } from '../base/types/types'

export interface WizardStep<T> {
  title: string
  fields: FormFieldDef<T>[]
}

const props = defineProps<{
  steps: WizardStep<T>[]
}>()

const emit = defineEmits<{
  (e: 'finish'): void
}>()

const form = inject(FormContextKey)
if (!form) {
  throw new Error('WizardForm must be used inside FormProvider')
}

const currentStep = ref(0)

const isLastStep = computed(
  () => currentStep.value === props.steps.length - 1
)

const validateCurrentStep = () => {
  const fields = props.steps[currentStep.value]!.fields
  let valid = true

  for (const field of fields) {
    const ok = form.validateField(String(field.key))
    if (!ok) valid = false
  }

  return valid
}

const next = () => {
  if (!validateCurrentStep()) return

  if (!isLastStep.value) {
    currentStep.value++
  } else {
    const allValid = form.validateAll()
    if (allValid) emit('finish')
  }
}

const back = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}
</script>

<template>
  <div class="wizard-form">
    <!-- Step Indicator -->
    <div class="wizard-steps">
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="wizard-step"
        :class="{
          active: index === currentStep,
          completed: index < currentStep
        }"
      >
        <div class="wizard-circle">
          {{ index + 1 }}
        </div>
        <div class="wizard-title">
          {{ step.title }}
        </div>
      </div>
    </div>

    <!-- Current Step Fields -->
    <div class="wizard-content">
      <EditableFormCell
        v-for="field in steps[currentStep]!.fields"
        :key="String(field.key)"
        :field="field"
      />
    </div>

    <!-- Actions -->
    <div class="wizard-actions">
      <v-btn
        v-if="currentStep > 0"
        variant="outlined"
        @click="back"
      >
        Back
      </v-btn>

      <v-btn
        color="primary"
        @click="next"
      >
        {{ isLastStep ? 'Finish' : 'Next' }}
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.wizard-steps {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.wizard-step {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.5;
}

.wizard-step.active {
  opacity: 1;
  font-weight: bold;
}

.wizard-step.completed {
  opacity: 1;
  color: #1976d2;
}

.wizard-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.wizard-step.active .wizard-circle {
  background: #1976d2;
  color: white;
}

.wizard-step.completed .wizard-circle {
  background: #1976d2;
  color: white;
}

.wizard-actions {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
}
</style>