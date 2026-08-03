import { L10n, registerLicense, setCulture } from '@syncfusion/ej2-base'
import syncfusionJapaneseLocale from '@syncfusion/ej2-locale/src/ja.json'

let configured = false
let localeConfigured = false

const spreadsheetJapaneseOverrides = {
  Open: '開く',
  Save: '保存',
  Cut: '切り取り',
  Paste: '貼り付け',
  Bold: '太字',
  Borders: '罫線',
  Sort: '並べ替え',
  SortAscending: '昇順',
  ReapplyFilter: 'フィルターを再適用',
  ReplaceBtn: '置換',
  FindNextBtn: '次を検索',
  FreezePanes: 'ウィンドウ枠の固定',
  FreezeRows: '行の固定',
  FreezeColumns: '列の固定',
  UnfreezePanes: 'ウィンドウ枠の固定を解除',
  Hide: '非表示',
  Ok: 'OK',
  Close: '閉じる',
  Apply: '適用',
  General: '標準',
  Number: '数値',
  ShortDate: '短い日付',
  LongDate: '長い日付',
  Percentage: 'パーセント',
  Text: '文字列',
  ProtectSheet: 'シートの保護',
  ClearFormats: '書式をクリア',
  MergeCells: 'セルの結合',
  MergeAll: 'すべて結合',
}

function configureJapaneseLocale() {
  if (localeConfigured) return

  L10n.load({
    ja: {
      ...syncfusionJapaneseLocale.ja,
      spreadsheet: {
        ...syncfusionJapaneseLocale.ja.spreadsheet,
        ...spreadsheetJapaneseOverrides,
      },
    },
  })
  setCulture('ja')
  localeConfigured = true
}

export function configureSyncfusion(): boolean {
  configureJapaneseLocale()

  if (configured) return true

  const licenseKey = import.meta.env.VITE_SYNCFUSION_LICENSE_KEY?.trim()

  if (!licenseKey) {
    return false
  }

  registerLicense(licenseKey)
  configured = true
  return true
}
