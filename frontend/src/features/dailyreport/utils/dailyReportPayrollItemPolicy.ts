export const LEGAL_DEPOSIT_CODE = 'LEGAL_DEPOSIT'

export const LEGAL_DEPOSIT_SYSTEM_OVERRIDE_REASON =
  '日報入力による法定準備金手動調整'

export const requiresManualOverrideReason = (
  code: string,
  manualOverride: boolean,
): boolean =>
  manualOverride && code !== LEGAL_DEPOSIT_CODE

export const resolveManualOverrideReason = (
  code: string,
  manualOverride: boolean,
  reason: string | null | undefined,
): string | null => {
  if (!manualOverride) return null

  const normalized = reason?.trim() ?? ''
  if (normalized) return normalized

  return code === LEGAL_DEPOSIT_CODE
    ? LEGAL_DEPOSIT_SYSTEM_OVERRIDE_REASON
    : null
}
