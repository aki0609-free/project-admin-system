export type SearchPanelFieldType =
  | 'text'
  | 'select'
  | 'date'
  | 'month'
  | 'number'

export type SearchPanelOption = {
  title: string
  value: string | number | boolean | null
}

export type SearchPanelModel = Record<string, unknown>

export type SearchPanelFieldDef<TModel extends SearchPanelModel = SearchPanelModel> = {
  key: keyof TModel & string
  label: string
  type: SearchPanelFieldType
  placeholder?: string
  options?: SearchPanelOption[]
  cols?: number
  md?: number
  clearable?: boolean
  prependIcon?: string
}