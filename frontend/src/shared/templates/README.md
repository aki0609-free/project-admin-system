# Shared page templates

This directory contains the canonical page compositions used across domains.

- `list-detail`: page title, toolbar, search area, table/content area, and dialogs
- Domain-specific API calls and business state stay in each feature composable.
- Existing `toolbox` components remain compatibility assets only while runtime imports exist.
- A legacy component is removed only after its reference count reaches zero and regression tests pass.
- Spreadsheet, ledger, report preview, and multi-level pivot internals remain domain-specific UI.
