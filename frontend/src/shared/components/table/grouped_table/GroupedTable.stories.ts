import type { Meta, StoryObj } from '@storybook/vue3'
import GroupedTable from './GroupedTable.vue'

/* =========================
   型
========================= */

type RowData = {
  id: number
  name: string
  department: string
  salary: number
  bonus: number
}

/* =========================
   データ
========================= */

const data: RowData[] = [
  { id: 1, name: '山田太郎', department: '営業', salary: 300000, bonus: 50000 },
  { id: 2, name: '鈴木一郎', department: '営業', salary: 280000, bonus: 30000 },
  { id: 3, name: '中村誠', department: '営業', salary: 420000, bonus: 120000 },

  { id: 4, name: '佐藤花子', department: '開発', salary: 350000, bonus: 80000 },
  { id: 5, name: '高橋健', department: '開発', salary: 320000, bonus: 60000 },
  { id: 6, name: '加藤沙織', department: '開発', salary: 360000, bonus: 75000 },

  { id: 7, name: '伊藤美咲', department: '人事', salary: 400000, bonus: 100000 },
  { id: 8, name: '渡辺直樹', department: '人事', salary: 310000, bonus: 45000 },
]

/* =========================
   定義（groupBy除外）
========================= */
type Aggregation = 'sum' | 'avg' | 'count'
type Column<T> = {
  key: keyof T
  title: string
  formatter?: (value: unknown, row: T) => string
  aggregation?: Aggregation
}

type Def<T> = {
  row: {
    key: keyof T
    title: string
  }
  columns: Column<T>[]
}

const def: Def<RowData> = {
  row: {
    key: 'name',
    title: '名前',
  },
  columns: [
    {
      key: 'name',
      title: '名前',
      aggregation: 'count',
    },
    {
      key: 'salary',
      title: '給料',
      aggregation: 'sum',
      formatter: (v: unknown) =>
        Number(v || 0).toLocaleString() + '円',
    },
    {
      key: 'bonus',
      title: 'ボーナス',
      aggregation: 'avg',
      formatter: (v: unknown) =>
        Number(v || 0).toLocaleString() + '円',
    },
  ],
}

/* =========================
   Storybook
========================= */

const meta: Meta<typeof GroupedTable<RowData>> = {
  title: 'Components/Tables/GroupedTable',
  component: GroupedTable as any,
}

export default meta
type Story = StoryObj<typeof GroupedTable>

/* =========================
   Default
========================= */

export const Default: Story = {
  args: {
    items: data,
    def,
    groupBy: 'department' as any,
  },
}

/* =========================
   グループ切替
========================= */

export const GroupByName: Story = {
  args: {
    items: data,
    def,
    groupBy: 'name' as any,
  },
}

/* =========================
   グループなし
========================= */

export const NoGrouping: Story = {
  args: {
    items: data,
    def,
    groupBy: null as any,
  },
}

/* =========================
   単一グループ
========================= */

export const SingleGroup: Story = {
  args: {
    items: data.filter(d => d.department === '営業'),
    def,
    groupBy: 'department' as any,
  },
}

/* =========================
   空データ（重要テスト）
========================= */

export const Empty: Story = {
  args: {
    items: [],
    def,
    groupBy: 'department' as any,
  },
}