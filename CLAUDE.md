# CLAUDE.md — Claude Code 運用ルール

このプロジェクトのメイン実装は **Codex** が担当する。Claude Code の役割は
**トークン消費を最小限に抑えながら、既存仕様書とソースコードを段階的に解析・ドキュメント化すること** に限定する。

## 1. 役割と権限（厳守）

- ソースコード（`backend/`, `frontend/`, `infrastructure/`, `openapi/`, `report-design/` ほか）の **編集・作成は禁止**。
  ユーザーから明確な実装指示があった場合のみ例外とする。
- 作成・編集してよいのは以下のみ:
  - `.claude/` 配下
  - `docs/claude-analysis/` 配下
  - `docs/HANDOFF.md`
- `docs/specs/`, `docs/kb/` の既存仕様書は **読み取り専用**（乖離は解析結果側に記録する）。
- git の commit / push / ブランチ操作はユーザーの指示があるまで行わない。

## 2. インクリメンタル（段階的）解析

- プロジェクト全体への無差別な `grep` / `find` / 全ファイル読み込みは **禁止**。
- 1回の解析は **1ディレクトリ／1機能単位** のみ。対象はユーザー指定、または `docs/claude-analysis/_INDEX.md` の次の未着手項目。
- 読み込むのは「対象ディレクトリのソース」＋「対象に対応する `docs/specs/<機能>/`・関連 `docs/kb/`」に限る。
- 大きなファイルは必要範囲のみ（offset/limit）読む。生成物・ビルド成果物（`node_modules`, `storybook-static`, `playwright-report`, `test-results`, `output*`, `tmp`, `.workspace-backups`, `.terraform` 等）は読まない。

## 3. 出力と引き継ぎ

1. 解析結果はチャットに長々と出さず、`docs/claude-analysis/<対象名>.md` に構造化して保存する。
   - 処理フロー・依存関係・ER は Mermaid 記法を活用する。
   - 推奨構成: 概要 / 構成ファイル / エンドポイント・画面 / 処理フロー / DB操作（テーブル・CRUD） / 依存関係 / 既存仕様書との乖離 / 未解決の疑問。
2. 保存後に更新する:
   - `docs/claude-analysis/_INDEX.md` の該当行ステータス（`[ ]` → `[x]`、日付、成果物リンク）
   - `docs/HANDOFF.md` の「最新進捗」（1〜2行）
3. チャットへの報告は以下 **のみ**:
   - 作成したファイルパス
   - 要点（3〜5行）
   - 次に解析すべき候補

## 4. サブエージェント活用

- コード解析はメインのコンテキストを汚さないよう、読み取り専用サブエージェント
  `code-analyzer`（`.claude/agents/code-analyzer.md`）に委譲し、構造化された Markdown 要約のみを受け取る。
- 委譲時は「対象ディレクトリ」「対応する仕様書パス」を明示し、範囲外を読ませない。
- 受け取った要約をメイン側で `docs/claude-analysis/<対象名>.md` に保存する。

## 5. 参照

- 解析計画表: `docs/claude-analysis/_INDEX.md`
- Codex との引き継ぎ: `docs/HANDOFF.md`
- 既存仕様書: `docs/specs/`（機能別）, `docs/kb/`（設計判断・ナレッジ）
