# 応募者管理 V1仕様

## V1の範囲

- 応募者の登録・更新・削除
- 応募情報、媒体情報、属性情報、入退社情報の表示
- 応募月、媒体、在籍・退職ステータス、性別、雇用形態によるChart絞り込み
- Chartと総計による応募・面接・在籍・退職状況の確認
- AI分析は未接続とし、実行ボタンを無効化する

## 集計定義

| 指標 | 定義 |
|---|---|
| 総応募者数 | 絞り込み対象となる応募者の件数 |
| 面接実施数 | 採用状況が `INTERVIEW`、`HIRED`、`BACKOUT` の件数 |
| 面接率 | 面接実施数 ÷ 総応募者数 × 100（四捨五入） |
| 在籍中人数 | 退職状況が `WORKING` の件数 |
| 在籍者率（応募者比） | 在籍中人数 ÷ 総応募者数 × 100（四捨五入） |
| 退職人数 | 退職状況が `RESIGNED` または `BACKOUT` の件数 |

採用状況は現在の到達地点を保持するため、`HIRED`と`BACKOUT`も面接実施済みとして扱う。
月別集計は問い合わせ日の年月を基準とする。したがって、月別在籍中人数・退職人数は「その月に応募した人の現在の状態」を表す。

## 入力・更新時のルール

- 応募者番号は、現在の最大番号に1を加えた値を新規画面へ初期表示する。
- 氏名と応募者番号は必須とする。
- PUT更新では、退職日や退職理由などの任意項目を空欄へ戻せる。
- Chartの分母が0の場合、割合は `0%` とし、`NaN`を表示しない。

## 主なコード

- Frontend画面: `frontend/src/features/application/pages/ApplicantView.vue`
- Chart集計: `frontend/src/features/application/composables/applicant/useApplicantChartSummary.ts`
- 総計: `frontend/src/features/application/composables/applicant/useApplicantAnalysisSummary.ts`
- 共通判定: `frontend/src/features/application/utils/applicantMetrics.ts`
- Backend更新: `backend/src/main/java/com/project/backend/features/application/service/ApplicantCommandService.java`
- Entity変換: `backend/src/main/java/com/project/backend/features/application/mapper/ApplicantMapper.java`
