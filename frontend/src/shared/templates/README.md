# Shared page templates

This directory contains the canonical page compositions used across domains.

- `list-detail`: page title, toolbar, search area, table/content area, and dialogs
- Domain-specific API calls and business state stay in each feature composable.
- Existing `toolbox` components remain compatibility assets until every import is migrated.
- A legacy component is removed only after its reference count reaches zero and regression tests pass.
