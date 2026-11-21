# LinkView Agent Guidelines & Context Reference

This document provides essential architectural context, engineering invariants, and lessons learned from past debugging sessions to help future agents maintain and extend LinkView effectively.

---

## 1. Project Overview & Tech Stack

LinkView is an offline-first, bidirectional linked note-taking application designed for Android tablets and phones.
- **Language**: Kotlin 2.0.20
- **UI Toolkit**: Jetpack Compose with Material 3 (Compose BOM `2024.09.03`)
- **Architecture**: Clean Architecture with Multi-Module Gradle setup (MVVM + StateFlow + Use Cases)
- **Dependency Injection**: Dagger Hilt
- **Persistence**: Room Database over native SQLite (`linkview.db`)
- **Serialization**: Kotlinx Serialization (polymorphic and custom envelope decoders)
- **Testing**: JUnit 5, MockK, Coroutines Test

### Module Structure
- `app`: Application shell, navigation graph, responsive split-pane scaffolding.
- `core:model`: Pure domain entities (`Card`, `ContextUnit`, `CardWithContext`, `BackupEnvelope`, `ReadableCard`).
- `core:common`: Utility classes, `MentionTokenizer`, `Result`, dispatcher providers.
- `core:ui`: Common Compose components, design system theme, `HighlightedText`, `RichMentionText`, `MarkdownUtils`.
- `domain`: Use cases and repository interfaces.
- `data:database`: Room database, entities, DAOs, type converters.
- `data:repository`: Repository implementations (`CardRepositoryImpl`, `SearchRepositoryImpl`).
- `data:backup`: Serializers, backup import/export engine (`BackupRepositoryImpl`, `JsonBackupSerializer`).
- `data:datastore`: Search preferences and settings storage.
- `feature:cards`: Card grid, search header, tag autocomplete, adaptive layout.
- `feature:detail`: Read-only card detail, Markdown rendering, in-place JSON editor.
- `feature:editor`: Card editor, multi-tag input, context unit reordering (Up/Down), `@mention` autocomplete.
- `feature:backup`: Export (Readable JSON, Full System Backup, Reference Titles), Import file picker, Paste JSON dialog.

---

## 2. Core Invariants & Architectural Rules

### Card Title Uniqueness & Collection Isolation
- Card title uniqueness is scoped per collection (case-insensitive).
- When a user saves or renames a card, check `checkDuplicateTitleUseCase`.
- Different collections/rooms are fully isolated contexts: cards, tags, backlinks, searches, and exports/imports operate exclusively within the active collection.
- When cards are created via mentions or imported from JSON, always reuse `existingCard.id` if a card with the same title already exists in the same collection. Never generate a new UUID for an existing title.

### Two-Pass Import Algorithm
- When importing JSON (whether a full system backup or human-readable array):
  - **Pass 1 (Discovery & Upsert)**: Insert/upsert all cards defined in the file *first* and map `titleToIdMap[lowercaseTitle] = cardId`.
  - **Pass 2 (Context Units & Linking)**: Insert context units and wire up references.
  - *Why this is critical*: In a single-pass loop, if Card 1 mentions Card 2 (a forward reference), a premature stub is created for Card 2. When Card 2 is reached later in the file, generating a new UUID will insert a duplicate card into the database. Pass 1 guarantees all declared cards exist before references are wired.

### Inline Mention Typography (`RichMentionText`)
- Do **not** use `FlowRow` to mix `Text` and `Surface` chips for inline `@mentions`.
- Mixing `Text` and non-text composables in `FlowRow` causes:
  1. Punctuation (commas, periods) to break onto a new line alone when a mention reaches the right edge.
  2. The mention chip to hang down below the text line baseline.
  3. Excessive horizontal spacing (padding on chip + FlowRow gap).
- **Rule**: Always render inline mentions within a single `AnnotatedString` using Compose 1.7's `LinkAnnotation.Clickable`. Mention spans sit directly on the text baseline, and trailing punctuation remains attached to the mention.

### Grid Row Height Equalization
- `LazyVerticalGrid` measures items independently, resulting in uneven card heights across the same row.
- In `CardsGridContent`, equal height is achieved by:
  1. Measuring `maxWidth` via `BoxWithConstraints` and calculating column count (`columns = maxOf(1, availableWidth / minCardSize)`).
  2. Chunking cards into rows (`cards.chunked(columns)`).
  3. Rendering with `LazyColumn` where each row is a `Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max))`.
  4. Wrapping each card in `Box(modifier = Modifier.weight(1f).fillMaxHeight())`.
  5. Using `verticalArrangement = Arrangement.SpaceBetween` inside the card Column so titles stay at the top and tags anchor at the bottom.

### Header Search Bar Sizing
- Search bars in `CardsScreen` must use `CompactSearchBar` with fixed `44.dp` height.
- Default Material 3 `OutlinedTextField` enforces a `56.dp` minimum height, which visually clashes with adjacent 44dp circular action buttons (New Card `+` and Backup).

### Markdown Formatting Rules
- Context units support:
  - Unordered bullet lists: lines starting with `- ` or `* ` (preserving indentation).
  - Bold: `**text**`
  - Italic: `*text*`
  - Bold + Italic: `***text***`
  - Inline code: `` `code` ``
  - Strikethrough: `~~text~~`
- Punctuation, math symbols (e.g. `5 - 3 = 2`), and negative numbers (e.g. `-10`) must not be converted into bullet points.

---

## 3. UI Guidelines & Constraints

- **No Emojis**: Do not use emojis in UI text, button labels, toasts, dialogs, or documentation files (`README.md`, `TODO.md`, `AGENT.md`). Keep all typography clean and professional.
- **No AI Phrasing in UI**: Do not mention "AI" in user-facing UI copy. Use neutral, domain-agnostic terminology:
  - Use "Reference Index" instead of "AI Grounding".
  - Use "canonical catalog of card titles" or "external drafting and notes".
  - Use "shorthand units: List<String>" instead of "AI-friendly units".

---

## 4. Local SQLite Database Schema (Version 3)

The database file is located at `/data/data/com.linkview.app/databases/linkview.db`.

1. **`collections`**:
   - `id`: TEXT (UUID, Primary Key, default room id is `"default"`)
   - `name`: TEXT (Unique, NOCASE)
   - `createdAt`: INTEGER
   - `updatedAt`: INTEGER
2. **`cards`**:
   - `id`: TEXT (UUID, Primary Key)
   - `title`: TEXT (Collate NOCASE, indexed)
   - `type`: TEXT (Nullable)
   - `tagsCsv`: TEXT
   - `createdAt`: INTEGER
   - `updatedAt`: INTEGER
   - `collectionId`: TEXT (Foreign Key -> `collections(id)`, `ON DELETE CASCADE`, indexed, default `"default"`)
3. **`context_units`**:
   - `id`: TEXT (UUID, Primary Key)
   - `source_card_id`: TEXT (Foreign Key -> `cards(id)`, `ON DELETE CASCADE`)
   - `sort_order`: INTEGER
   - `raw_text`: TEXT
   - `plain_text`: TEXT
4. **`unit_references`** (Junction Table):
   - `unit_id`: TEXT (Foreign Key -> `context_units(id)`, `ON DELETE CASCADE`)
   - `target_card_id`: TEXT (Foreign Key -> `cards(id)`, `ON DELETE CASCADE`)
   - Primary Key: `(unit_id, target_card_id)`
5. **`card_tags`**:
   - `card_id`: TEXT (Foreign Key -> `cards(id)`, `ON DELETE CASCADE`)
   - `tag`: TEXT
   - Primary Key: `(card_id, tag)`

---

## 5. Development & Terminal Execution Rules (Windows Host)

- **PowerShell Separators**: Always use semicolon (`;`) instead of `&&` when chaining commands in PowerShell.
- **Gradle Execution**: Always run Gradle with `BypassSandbox: true` because Windows file-locking on daemon wrappers requires elevated permissions.
- **Network Access**: Running `git push` requires `BypassSandbox: true`.
- **Build & Verification Command**:
  ```powershell
  cd linkview; ./gradlew.bat testDebugUnitTest assembleDebug --daemon
  ```
- **Output APK Path**:
  `linkview/app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Common Pitfalls & Solutions

1. **NPE in `MainActivity.kt` during navigation animation**:
   - *Issue*: During exit transition of `AnimatedContent`, `activeCardId` or `editingCardId` may already be set to `null` by `onNavigateBack()`.
   - *Fix*: Never call `activeCardId!!` or `editingCardId!!`. Extract IDs safely from `targetState` (e.g. `target.removePrefix("detail_")`).
2. **Room duplicate insertions on REPLACE_ALL import**:
   - *Issue*: Stubs created during reference linking get new UUIDs if not mapped in Pass 1.
   - *Fix*: Always populate `titleToIdMap` in Pass 1 and check `cardDao.getCardByExactTitle` before assigning a UUID.
3. **OutlinedTextField height clipping**:
   - *Issue*: Constraining `OutlinedTextField` to 44dp causes vertical text clipping due to internal 16dp padding.
   - *Fix*: Use `CompactSearchBar` built with `BasicTextField` inside a 44dp `Surface`.
4. **Foreign Key Cascade Deletion with `OnConflictStrategy.REPLACE`**:
   - *Issue*: In SQLite, `INSERT OR REPLACE` deletes the conflicting row before inserting the new one. Because child tables like `unit_references(targetCardId)` declare `ON DELETE CASCADE`, updating a card with `REPLACE` deletes all incoming backlinks pointing to that card from other cards.
   - *Fix*: Never use `@Insert(onConflict = OnConflictStrategy.REPLACE)` for parent entities. Always use Room's `@Upsert` annotation, which generates `INSERT ... ON CONFLICT DO UPDATE` and modifies the row in-place without triggering foreign key cascades.
