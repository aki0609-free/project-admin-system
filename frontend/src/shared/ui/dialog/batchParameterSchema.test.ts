import { describe, expect, it } from 'vitest'
import { buildBatchParameterSchema } from './batchParameterSchema'

describe('buildBatchParameterSchema', () => {
  const employeeDefinition = {
    key: 'employeeId',
    label: '従業員',
    type: 'select' as const,
    required: true,
    options: [
      { title: 'E2E-EMP-001 / 給与検証社員', value: 101 },
      { title: 'E2E-EMP-002 / 給与検証社員2', value: 102 },
    ],
  }

  it('数値IDを持つ選択肢を受け付ける', () => {
    const schema = buildBatchParameterSchema([employeeDefinition])

    expect(schema.safeParse({ employeeId: 101 }).success).toBe(true)
  })

  it('選択肢にない値を拒否する', () => {
    const schema = buildBatchParameterSchema([employeeDefinition])

    const result = schema.safeParse({ employeeId: 999 })
    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('従業員の選択値が不正です')
    }
  })

  it('必須の選択が空の場合は拒否する', () => {
    const schema = buildBatchParameterSchema([employeeDefinition])

    expect(schema.safeParse({ employeeId: '' }).success).toBe(false)
  })

  it('任意の選択は未選択のままでも受け付ける', () => {
    const schema = buildBatchParameterSchema([
      { ...employeeDefinition, required: false },
    ])

    expect(schema.safeParse({ employeeId: '' }).success).toBe(true)
  })

  it('文字列と真偽値の選択肢も従来どおり受け付ける', () => {
    const schema = buildBatchParameterSchema([
      {
        key: 'mode',
        label: '出力区分',
        type: 'select',
        required: true,
        options: [{ title: '月次', value: 'MONTHLY' }],
      },
      {
        key: 'enabled',
        label: '有効状態',
        type: 'select',
        required: true,
        options: [{ title: '有効', value: true }],
      },
    ])

    expect(schema.safeParse({ mode: 'MONTHLY', enabled: true }).success).toBe(true)
  })
})
