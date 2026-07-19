import type { Meta, StoryObj } from '@storybook/vue3'
import MultiLevelHeaderTable from './MultiLevelHeaderTable.vue'
import type { MultiLevelHeaderTableDef } from './types/item/types'

type RowData = {
  id: number
  name: string
  birth: string
  salary: number
  bonus: number
  department: string
}

/* =========================
   データ
========================= */

const data: RowData[] = [
  { id: 1, name: '山田太郎', birth: '1990-01-01', salary: 300000, bonus: 50000, department: '営業' },
  { id: 2, name: '佐藤花子', birth: '1988-05-12', salary: 350000, bonus: 80000, department: '開発' },
  { id: 3, name: '鈴木一郎', birth: '1995-09-20', salary: 280000, bonus: 30000, department: '営業' },
  { id: 4, name: '高橋健', birth: '1992-03-15', salary: 320000, bonus: 60000, department: '開発' },
  { id: 5, name: '伊藤美咲', birth: '1987-07-08', salary: 400000, bonus: 100000, department: '営業' },
  { id: 6, name: '渡辺直樹', birth: '1993-11-23', salary: 310000, bonus: 45000, department: '開発' },
  { id: 7, name: '小林優', birth: '1991-02-17', salary: 290000, bonus: 35000, department: '営業' },
  { id: 8, name: '加藤沙織', birth: '1989-09-30', salary: 360000, bonus: 75000, department: '開発' },
  { id: 9, name: '吉田翔', birth: '1996-06-05', salary: 270000, bonus: 20000, department: '営業' },
  { id: 10, name: '山本彩', birth: '1994-12-12', salary: 330000, bonus: 55000, department: '開発' },

  { id: 11, name: '中村誠', birth: '1986-04-21', salary: 420000, bonus: 120000, department: '営業' },
  { id: 12, name: '松本葵', birth: '1997-08-14', salary: 260000, bonus: 25000, department: '開発' },
  { id: 13, name: '井上拓也', birth: '1990-10-10', salary: 340000, bonus: 65000, department: '営業' },
  { id: 14, name: '木村真理', birth: '1985-01-19', salary: 450000, bonus: 150000, department: '開発' },
  { id: 15, name: '清水健太', birth: '1992-05-25', salary: 310000, bonus: 40000, department: '営業' },
  { id: 16, name: '山崎奈々', birth: '1993-03-03', salary: 300000, bonus: 50000, department: '開発' },
  { id: 17, name: '森大輔', birth: '1988-11-11', salary: 370000, bonus: 90000, department: '営業' },
  { id: 18, name: '池田由美', birth: '1995-07-27', salary: 280000, bonus: 30000, department: '開発' },
  { id: 19, name: '橋本亮', birth: '1991-09-09', salary: 350000, bonus: 70000, department: '営業' },
  { id: 20, name: '阿部千尋', birth: '1996-02-02', salary: 275000, bonus: 22000, department: '開発' },
]

/* =========================
   共通filter定義
========================= */

const textFilter = { type: 'text' } as const
const numberFilter = { type: 'number' } as const
const selectFilter = { type: 'select' } as const
const dateFilter = { type: 'date' } as const

/* =========================
   定義
========================= */

const def: MultiLevelHeaderTableDef<RowData> = {
  row: {
    key: 'name',
    title: '名前',
    filter: {
      type: 'text'
    }
  },
  columns: [
    {
      key: 'personal' as any,
      title: '個人情報',
      subColumns: [
        {
          key: 'birth',
          title: '生年月日',
          editable: true,
          type: 'date',
          filter: dateFilter,
        },
        {
          key: 'department',
          title: '部署',
          editable: true,
          type: 'select',
          enumOptions: [
            { title: '営業', value: '営業' },
            { title: '開発', value: '開発' },
          ],
          filter: selectFilter,
        },
      ],
    },
    {
      key: 'money' as any,
      title: '給与情報',
      subColumns: [
        {
          key: 'salary',
          title: '給料',
          editable: true,
          type: 'number',
          formatter: (v) => Number(v || 0).toLocaleString() + '円',
          filter: numberFilter,
          aggregation: 'sum'
        },
        {
          key: 'bonus',
          title: 'ボーナス',
          editable: true,
          type: 'number',
          formatter: (v) => Number(v || 0).toLocaleString() + '円',
          filter: numberFilter,
          aggregation: 'sum'
        },
      ],
    },
  ],
}

/* =========================
   Storybook
========================= */

const meta: Meta<typeof MultiLevelHeaderTable> = {
  title: 'Components/Tables/MultiLevelHeaderTable',
  component: MultiLevelHeaderTable,
}

export default meta
type Story = StoryObj<typeof MultiLevelHeaderTable>

/* =========================
   Default
========================= */

export const Default: Story = {
  args: {
    data,
    def,
    tableKey: 'default-table',
  },
}

/* =========================
   フラット
========================= */

export const FlatColumns: Story = {
  args: {
    data,
    tableKey: 'flat-table',
    def: {
      row: { key: 'name', title: '名前', filter: { type: 'text' } },
      columns: [
        {
          key: 'birth',
          title: '生年月日',
          editable: true,
          type: 'date',
          filter: dateFilter,
        },
        {
          key: 'department',
          title: '部署',
          editable: true,
          type: 'select',
          enumOptions: [
            { title: '営業', value: '営業' },
            { title: '開発', value: '開発' },
          ],
          filter: selectFilter,
        },
        {
          key: 'salary',
          title: '給料',
          editable: true,
          type: 'number',
          filter: numberFilter,
          aggregation: 'sum'
        },
      ],
    },
  },
}

/* =========================
   深いネスト
========================= */

export const DeepNested: Story = {
  args: {
    data,
    tableKey: 'deep-table',
    def: {
      row: { key: 'name', title: '名前', filter: { type: 'text' } },
      columns: [
        {
          key: 'all' as any,
          title: '全体',
          subColumns: [
            {
              key: 'personal' as any,
              title: '個人',
              subColumns: [
                {
                  key: 'birth',
                  title: '生年月日',
                  editable: true,
                  type: 'date',
                  filter: dateFilter,
                },
                {
                  key: 'department',
                  title: '部署',
                  editable: true,
                  type: 'select',
                  enumOptions: [
                    { title: '営業', value: '営業' },
                    { title: '開発', value: '開発' },
                  ],
                  filter: selectFilter,
                },
              ],
            },
            {
              key: 'money' as any,
              title: 'お金',
              subColumns: [
                {
                  key: 'salary',
                  title: '給料',
                  editable: true,
                  type: 'number',
                  filter: numberFilter,
                  aggregation: 'sum'
                },
                {
                  key: 'bonus',
                  title: 'ボーナス',
                  editable: true,
                  type: 'number',
                  filter: numberFilter,
                  aggregation: 'avg'
                },
              ],
            },
          ],
        },
      ],
    },
  },
}