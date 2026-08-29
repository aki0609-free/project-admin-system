import type { Meta, StoryObj } from '@storybook/vue3'
import type { DefineComponent } from 'vue'
import SimpleTable from './SimpleTable.vue' 
import type { SimpleTableColumnDef, SimpleTableEditableRow } from './types/item/types' 
import type { SimpleTableFilterRule } from './types/filter/types'

type StoryComponentProps = {
  tableKey: string
  itemKey: keyof DemoTableRow
  items: DemoTableRow[]
  columns: SimpleTableColumnDef<DemoTableRow>[]
  filterRules: SimpleTableFilterRule<DemoTableRow>[]
}

const StoryComponent = SimpleTable as unknown as DefineComponent<
  StoryComponentProps
>

const meta: Meta<typeof StoryComponent> = {
  title: 'Components/Tables/SimpleTable',
  component: StoryComponent,
}

type DemoTableRow = SimpleTableEditableRow & {
    name: string
    age: number
    email: string
}

export default meta
type Story = StoryObj<typeof StoryComponent>

// サンプルデータ
const columns: SimpleTableColumnDef<DemoTableRow>[] = [
  { title: 'ID', key: 'id', type: 'text', width: '240px', filter: { type: 'text' } },
  { title: 'Name', key: 'name', type: 'text', width: '240px', filter: { type: 'text' } },
  { title: 'Age', key: 'age', type: 'text', width: '240px', filter: { type: 'text' }, editable: true, formatter: (v) => `${v}歳` },
  { title: 'Email', key: 'email', type: 'text', width: '240px', filter: { type: 'text' } },
]

const items: DemoTableRow[] = [
  { id: 1, name: 'Alice', age: 23, email: 'aaa@gmail.com' },
  { id: 2, name: 'Bob', age: 30, email: 'bbb@gmail.com' },
  { id: 3, name: 'Charlie', age: 28, email: 'ccc@gmail.com' },
]

const filterRules: SimpleTableFilterRule<DemoTableRow>[] =
  columns.map((col) => ({
    key: col.key,
    predicate: (item, value) =>
      String(item[col.key]).includes(String(value)),
  }))

export const Default: Story = {
  args: {
    tableKey: 'demo',
    itemKey: 'id',
    items,
    columns,
    filterRules,
  },
}
