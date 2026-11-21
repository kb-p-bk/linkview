# LinkView

LinkView is an offline-first, bidirectional linked note-taking application designed for Android tablets and phones. It is built natively with Jetpack Compose, Kotlin Coroutines, and Room SQLite.

## Overview

Traditional note-taking applications organize information into long, monolithic documents or strict hierarchical folders. LinkView organizes knowledge into atomic Cards containing modular Context Units. Every mention between cards creates an automatic bidirectional reference, splitting context into:

- Context: Authored notes written directly on the current card.
- Extra Context: Incoming backlinks from all other cards that mention this card.

## Core Features

- Atomic Cards and Modular Context Units: Each card represents a discrete topic, person, place, or concept, composed of ordered context units.
- Context Unit Reordering: Context units can be rearranged dynamically within the card editor using persistent Move Up and Move Down controls.
- Keyboard-Aware Mention Autocomplete: Typing `@` inside any context unit activates inline card autocomplete suggestions anchored cleanly above the text field, preventing the software keyboard from obscuring results.
- Automatic Card Stubbing: Referencing uncreated cards via mentions (`@[Card Title]`) or imported JSON automatically creates stub cards in the database and establishes incoming backlinks immediately.
- Rich Text Formatting: Context units render Markdown formatting including unordered bullet lists (`- ` or `* ` with optional indentation), **bold**, *italic*, `code`, and ~~strikethrough~~.
- Dual Export Portability: Export cards into clean Human-Readable JSON (titles, tags, units) for viewing and sharing, or Full System Backups (UUIDs, timestamps, and relational envelope) for complete device-to-device restores.
- Bidirectional Linking: Real-time relationship graphing distinguishing between direct card notes and incoming backlinks.
- Multi-Tag Filtering and AND-Queries: Assign multiple tags to cards with relational intersection search and dynamic tag autocompletion.
- Duplicate Title Prevention: Enforces card title uniqueness with duplicate detection and an instant "Open" navigation shortcut.
- Tablet-Optimized Split View: Responsive layout featuring a full-width search header and a 38-62 proportional split view between the card grid and card content on tablets and foldable devices.
- Declarative JSON Data Exchange: Clean JSON dialog for copying, inspecting, and editing card JSON directly, plus bulk paste ingestion and export in the Backup screen.
- Complete Offline SQLite Storage: All cards, context units, and relation graphs are stored locally on-device in SQLite using Android's Room library (`linkview.db`). No cloud servers, external accounts, or network permissions required.

## Declarative JSON Format and Data Exchange

LinkView provides a human-readable, declarative JSON format for fast data exchange, backups, and scripted imports without requiring callers to generate internal database identifiers (UUIDs) or timestamps. The application automatically provisions UUIDs, timestamps, and stub cards for any referenced targets.

### 1. Single Card Schema

Cards can be represented using this clean, human-readable schema:

```json
{
  "title": "Publius Rutilius Rufus",
  "tags": ["Person", "Late Republic", "Stoicism"],
  "units": [
    "Statesman, orator, and historian who served under @[Scipio Aemilianus] at Numantia.",
    "Formed a close intellectual partnership with the Stoic philosopher @[Panaetius].",
    "Unjustly condemned and exiled to @[Smyrna] in 92 BC on trumped-up extortion charges."
  ]
}
```

### 2. Multi-Card Collection Schema

Multiple cards can be grouped into an array for batch importing or backups:

```json
[
  {
    "title": "Publius Rutilius Rufus",
    "tags": ["Person", "Late Republic"],
    "units": [
      "Statesman, orator, and historian who served under @[Scipio Aemilianus] at Numantia.",
      "Exiled to @[Smyrna] on trumped-up extortion charges."
    ]
  },
  {
    "title": "Smyrna",
    "tags": ["Place", "Asia Minor"],
    "units": [
      "Major Aegean port city in ancient Ionia.",
      "Chosen by Rufus as his place of exile where he lived in high honor among the citizens."
    ]
  }
]
```

### Export Options and Portability

LinkView clearly distinguishes between two export formats on the Backup & Portability screen:

1. Readable Cards (.json):
   - Emits a clean JSON array of all cards with `title`, `tags`, and `units`.
   - Free of internal UUIDs, sortOrder, timestamps, or system wrapper objects.
   - Ideal for human reading, sharing, text editing, and scripting.
   - Can be saved as a file (`linkview-cards-readable-YYYY-MM-DD.json`) or copied directly to the clipboard.

2. Full System Backup (.json):
   - Emits a versioned envelope (`BackupEnvelope`) preserving exact internal UUIDs, timestamps, sort orders, and card types.
   - Ideal for lossless device-to-device migration and disaster recovery.
   - Saved as `linkview-full-backup-YYYY-MM-DD.json`.

3. Reference Titles Index:
   - Emits a compact index of existing card titles, either for all cards or filtered by selected tags.
   - One-tap copying as a Markdown list (`- Title 1\n- Title 2`) or JSON array (`["Title 1", "Title 2"]`).
   - Ideal for feeding into prompts so external tools know existing reference titles and link to them accurately.

### JSON Import and In-Place Editing

- View, Copy, and Edit Card JSON: Tap the code icon (`</>`) on any card's detail screen to inspect, copy, or directly edit its JSON representation. Saving changes in the dialog updates the card, its tags, and its context units in real time.
- Paste JSON Ingestion: Navigate to the Backup screen and tap "Paste JSON" to paste raw card JSON or arrays of cards directly into LinkView. LinkView automatically handles schema detection, UUID generation, and auto-stubbing.
- Mention Syntax: Mentions in context units use the `@` or `@[Target Title]` format. If the target card does not yet exist, LinkView automatically creates a new stub card for it.

## Local SQLite Database Architecture

All application data is persisted locally in an on-device SQLite database named `linkview.db`, managed via Android's first-party Room persistence library (`androidx.room`). No external databases or cloud networks are used.

The relational schema consists of four tables:

1. `cards`:
   - Primary key: `id` (UUID String)
   - Columns: `title` (unique per card), `type`, `tagsCsv`, `created_at`, `updated_at`

2. `context_units`:
   - Primary key: `id` (UUID String)
   - Foreign key: `source_card_id` referencing `cards(id)` with `ON DELETE CASCADE`
   - Columns: `sort_order`, `raw_text`, `plain_text`

3. `unit_references` (Junction Table):
   - Primary key: Composite `(unit_id, target_card_id)`
   - Foreign keys: `unit_id` referencing `context_units(id)` with `ON DELETE CASCADE`, and `target_card_id` referencing `cards(id)` with `ON DELETE CASCADE`
   - Purpose: Enables sub-millisecond reverse-lookup queries to find incoming backlinks ("Extra Context") without full table scans

4. `card_tags`:
   - Primary key: Composite `(card_id, tag)`
   - Foreign key: `card_id` referencing `cards(id)` with `ON DELETE CASCADE`
   - Purpose: Indexes card tags for fast intersection filtering and autocompletion

## Architecture

The project follows Clean Architecture and Android modularization best practices:

- app: Application shell, dependency injection setup, and top-level responsive navigation.
- core:model: Domain entities (Card, ContextUnit, CardWithContext, SearchResult, BackupEnvelope).
- core:common: Utilities, mention tokenizers, markdown parser, and common coroutine dispatchers.
- core:ui: Common Compose design system components, typography, rich mention renderer, and theme.
- domain: Repositories and business logic use cases.
- data:database: Room database, entities, DAOs, and relational queries.
- data:datastore: User preferences and search scope settings via Jetpack DataStore.
- data:repository: Implementations for card and search repositories with auto-stubbing support.
- data:backup: JSON backup serializer and importer supporting polymorphic envelope, single card, and card array formats.
- feature:cards: Card grid screen, full-width search header, and tag autocomplete.
- feature:detail: Card detail view with styled section chips for Context and Extra Context, Markdown rendering, in-place JSON editor, and copy export.
- feature:editor: Card editor, multi-tag input, context unit reordering (Up/Down), keyboard-safe @mention autocomplete, and duplicate validation.
- feature:backup: Backup creation, JSON file restoration, and direct JSON paste ingestion.

## Tech Stack

- Language: Kotlin 2.0
- UI: Jetpack Compose with Material 3
- Architecture: MVVM with Clean Architecture
- Dependency Injection: Dagger Hilt
- Database: Room with SQLite
- Concurrency: Kotlin Coroutines and StateFlow
- Serialization: Kotlinx Serialization

## Building and Running

Prerequisites:
- Android Studio Ladybug or newer
- JDK 17 or higher
- Android SDK with API 34+

Build Debug APK:
```bash
./gradlew assembleDebug
```

Run Unit Tests:
```bash
./gradlew testDebugUnitTest
```

The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`
