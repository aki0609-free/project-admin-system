<script setup lang="ts">
import { computed } from 'vue'

const model = defineModel<string>({
  default: '',
})

withDefaults(
  defineProps<{
    label?: string
    disabled?: boolean
    readonly?: boolean
    error?: boolean
    errorMessages?: string[]
  }>(),
  {
    label: '',
    disabled: false,
    readonly: false,
    error: false,
    errorMessages: () => [],
  },
)

const hour = computed({
  get: () => {
    const [hh] = normalizeTime(model.value).split(':')
    return hh
  },
  set: (value: string) => {
    model.value = `${normalizeHour(value)}:${minute.value}`
  },
})

const minute = computed({
  get: () => {
    const [, mm] = normalizeTime(model.value).split(':')
    return mm
  },
  set: (value: string) => {
    model.value = `${hour.value}:${normalizeMinute(value)}`
  },
})

const normalizeTime = (value: string): string => {
  if (!value) return '00:00'

  const match = value.match(/^(\d{1,2}):(\d{1,2})/)
  if (!match) return '00:00'

  return `${normalizeHour(match[1] as string)}:${normalizeMinute(match[2] as string)}`
}

const normalizeHour = (value: string): string => {
  const number = Number(value)
  if (Number.isNaN(number)) return '00'

  return String(Math.min(Math.max(number, 0), 23)).padStart(2, '0')
}

const normalizeMinute = (value: string): string => {
  const number = Number(value)
  if (Number.isNaN(number)) return '00'

  return String(Math.min(Math.max(number, 0), 59)).padStart(2, '0')
}
</script>

<template>
  <div class="time-form-field">
    <div class="time-label">
      {{ label }}
    </div>

    <div class="time-inputs">
      <v-text-field
        v-model="hour"
        type="number"
        min="0"
        max="23"
        density="compact"
        variant="outlined"
        hide-details
        class="time-part"
        :disabled="disabled"
        :readonly="readonly"
        :error="error"
      />

      <span class="time-separator">:</span>

      <v-text-field
        v-model="minute"
        type="number"
        min="0"
        max="59"
        density="compact"
        variant="outlined"
        hide-details
        class="time-part"
        :disabled="disabled"
        :readonly="readonly"
        :error="error"
      />
    </div>

    <div
      v-if="errorMessages.length"
      class="time-error"
    >
      {{ errorMessages[0] }}
    </div>
  </div>
</template>

<style scoped>
.time-form-field {
  display: grid;
  gap: 4px;
}

.time-label {
  font-size: 12px;
  color: #64748b;
}

.time-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}

.time-part {
  max-width: 72px;
}

.time-part :deep(input) {
  text-align: center;
  font-weight: 600;
}

.time-separator {
  font-weight: 700;
  color: #475569;
}

.time-error {
  font-size: 12px;
  color: #b00020;
}
</style>