# Claude 解析計画表（_INDEX）

1行 = 1回の解析単位。上から順に着手する（ユーザー指定があればそちらを優先）。
ステータス: `[ ]` 未着手 / `[~]` 着手中 / `[x]` 完了

BE = `backend/src/main/java/com/project/backend/`, FE = `frontend/src/`

## Phase 0: 横断基盤（他機能の前提）

| # | 状態 | 対象名 | ソース | 関連仕様書 | 成果物 | 完了日 |
|---|---|---|---|---|---|---|
| 0-1 | [ ] | auth-security | BE `app/security`, `app/config` | kb/v1-hotfix-to-post-v1-refactor-handoff-20261110.md | | |
| 0-2 | [x] | tenant-boundary | BE `app/tenant`, `app/base`, `app/persistence`, `common/filter` | kb/tenant-boundary-v1.md | [tenant-boundary.md](tenant-boundary.md) | 2026-11-15 |
| 0-2b | [x] | raw-sql-audit | native 6ファイル + JdbcTemplate 19ファイル。**業務正確性（削除・状態条件、計算）を主、tenant_id 条件は従**で監査 | — | [raw-sql-audit.md](raw-sql-audit.md) | 2026-11-15 |
| 0-3 | [ ] | audit-persistence-sql | BE `app/audit`, `app/persistence`, `app/sql` | — | | |
| 0-4 | [ ] | storage-file | BE `app/storage`, `app/file` | kb/document-management-s3-*.md | | |
| 0-5 | [ ] | common-utilities | BE `common/*`（closing, dayrule, error, exception ほか） | — | | |
| 0-6 | [ ] | fe-shared-and-app | FE `shared/`, `app/` | kb/frontend-shared-ui-refactoring-policy.md | | |

## Phase 1: 業務機能（Backend features ↔ Frontend features ↔ docs/specs）

| # | 状態 | 対象名 | ソース | 関連仕様書 | 成果物 | 完了日 |
|---|---|---|---|---|---|---|
| 1-1 | [x] | employee | BE `features/employee`, FE `features/employees` | specs/employee/ | [employee.md](employee.md) | 2026-11-15 |
| 1-2 | [ ] | customer | BE `features/customer`, FE `features/customer` | specs/customer/ | | |
| 1-3 | [x] | daily-report | BE `features/dailyreport`, FE `features/dailyreport` | specs/daily-report/ | [daily-report.md](daily-report.md) | 2026-11-15 |
| 1-4 | [ ] | dynamic-field | BE `features/dynamicfield`, FE `features/dynamicfield` | kb/daily-report-dynamic-payroll-items-rule-v1.md | | |
| 1-5 | [x] | payroll | BE `features/payroll`, `features/master/payrollitem`, FE `features/master/payrollitem`, `resources/sql/payroll` | specs/allowance/, specs/deduction/, kb/payroll-item-transaction-deduction-foundation.md | [payroll.md](payroll.md) | 2026-11-15 |
| 1-5b | [ ] | allowance-deduction-master | BE `features/master/{allowance,deduction}`, FE 同 | specs/allowance/, specs/deduction/ | | |
| 1-6 | [ ] | tax | BE `features/tax`, `schedule/tax` | kb/tax-data-import-v1.md | | |
| 1-7a | [x] | monthly-closing | BE `features/operation/monthly`, `common/closing`, FE `operation/monthly`, `operation/customer-billing`, `sql/operation/monthly*` | specs/monthly-closing/, specs/customer-billing-closing/ | [monthly-closing.md](monthly-closing.md) | 2026-11-15 |
| 1-7b | [ ] | daily-closing | BE `operation/daily`, FE `operation/daily`, `sql/operation/daily_closing` | specs/daily-closing/ | | |
| 1-7c | [ ] | daily-preparation | BE `operation/preparation`, FE `operation/preparation` | specs/daily-preparation/ | | |
| 1-7d | [ ] | book-operation | BE `operation/book`, FE `operation/book` | specs/book-operation/ | | |
| 1-7e | [ ] | report-preview | BE `operation/reportpreview`, FE `operation/reportpreview` | specs/report-preview/ | | |
| 1-8 | [ ] | master | BE `features/master`, FE `features/master` | specs/business-settings/ | | |
| 1-9 | [ ] | system-report | BE `features/system`, FE `features/system` | specs/report-preview/, specs/closing-reports/ | | |
| 1-10 | [ ] | application | BE `features/application`, `features/analysis`, FE `features/application` | specs/application/ | | |
| 1-11 | [ ] | admin-user | BE `features/admin`, `features/user`, FE `features/admin`, `features/users` | — | | |
| 1-12 | [ ] | dashboard | BE `features/dashboard`, FE `features/dashboard` | kb/dashboard-notice-v1-refactoring.md | | |

※ 1機能が大きい場合（例: operation, system）は解析着手時にサブ単位（1-7a, 1-7b…）へ分割して追記する。

## Phase 2: バッチ・SQL・帳票・インフラ

| # | 状態 | 対象名 | ソース | 関連仕様書 | 成果物 | 完了日 |
|---|---|---|---|---|---|---|
| 2-1 | [ ] | batch | BE `batch/` | kb/batch-processing-v1-refactoring.md | | |
| 2-2 | [ ] | sql-resources | `backend/src/main/resources/sql/`（サブディレクトリ単位） | — | | |
| 2-3 | [ ] | report-templates | `backend/src/main/resources/reports`, `report-design/jasper` | specs/closing-reports/ | | |
| 2-4 | [ ] | infrastructure | `infrastructure/modules`, `environments` | specs/infrastructure/ | | |
| 2-5 | [ ] | openapi | `openapi/` | — | | |

## Phase 3: 横断まとめ

| # | 状態 | 対象名 | 内容 | 成果物 | 完了日 |
|---|---|---|---|---|---|
| 3-1 | [ ] | spec-gap-summary | 各成果物の「仕様書との乖離」集約 | | |
| 3-2 | [ ] | dependency-map | 機能間依存の全体 Mermaid 図 | | |
