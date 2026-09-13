import { z } from 'zod'
import type { BatchParameterDefinition } from '@/shared/ui/toolbar/types'

const primitiveSelectValueSchema = z.union([
  z.string(),
  z.number(),
  z.boolean(),
])

const createFieldSchema = (definition: BatchParameterDefinition): z.ZodTypeAny => {
  let rule: z.ZodTypeAny

  switch (definition.type) {
    case 'number':
      rule = z.coerce.number()
      break
    case 'checkbox':
      rule = z.boolean()
      break
    case 'select':
      rule = primitiveSelectValueSchema.refine(
        value =>
          (typeof value === 'string' && value.trim() === '') ||
          !definition.options?.length ||
          definition.options.some(option => option.value === value),
        `${definition.label}の選択値が不正です`,
      )
      break
    default:
      rule = z.string()
      break
  }

  if (!definition.required) {
    return rule.optional()
  }

  if (definition.type === 'checkbox') {
    return rule
  }

  return rule.refine(
    value => value !== undefined && value !== null && String(value).trim() !== '',
    `${definition.label}は必須です`,
  )
}

export const buildBatchParameterSchema = (
  definitions: BatchParameterDefinition[],
) => {
  const shape: Record<string, z.ZodTypeAny> = {}

  for (const definition of definitions) {
    shape[definition.key] = createFieldSchema(definition)
  }

  return z.object(shape)
}
