export const resolveHttpsUrl = (value: string | undefined): string | null => {
  const normalized = value?.trim()
  if (!normalized) {
    return null
  }

  try {
    const url = new URL(normalized)
    return url.protocol === 'https:' ? url.toString() : null
  } catch {
    return null
  }
}

const defaultSupportLinks = {
  manual:
    'https://projectadmin1215.atlassian.net/wiki/spaces/~712020d0db24f25d734730b24dfb1508d24613/folder/19464193',
  incident:
    'https://projectadmin1215.atlassian.net/jira/software/projects/FUYO/form/1?atlOrigin=eyJpIjoiYTFkN2E2NWU2YWYwNGQ2ODk4MDRmMTliM2JkMjQ3YjgiLCJwIjoiaiJ9',
} as const

export const externalSupportLinks = Object.freeze({
  manual: resolveHttpsUrl(
    import.meta.env.VITE_CONFLUENCE_MANUAL_URL ?? defaultSupportLinks.manual,
  ),
  incident: resolveHttpsUrl(
    import.meta.env.VITE_JIRA_INCIDENT_FORM_URL ??
      defaultSupportLinks.incident,
  ),
})
