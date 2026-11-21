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
- Rich Text Formatting: Context units render inline Markdown formatting including **bold**, *italic*, `code`, and ~~strikethrough~~.
- Bidirectional Linking: Real-time relationship graphing distinguishing between direct card notes and incoming backlinks.
- Multi-Tag Filtering and AND-Queries: Assign multiple tags to cards with relational intersection search and dynamic tag autocompletion.
- Duplicate Title Prevention: Enforces card title uniqueness with duplicate detection and an instant "Open" navigation shortcut.
- Tablet-Optimized Split View: Responsive layout featuring a full-width search header and a 38-62 proportional split view between the card grid and card content on tablets and foldable devices.
- AI-Friendly JSON Workflows: One-tap JSON copy dialog in card details and direct JSON pasting in the Backup screen for seamless interoperability with LLMs and AI agents.
- Complete Offline Privacy: All cards, context units, and relation graphs are stored locally in private SQLite storage using Room. No external servers or network access required.

## AI Integration and JSON Schemas

LinkView supports seamless data exchange with AI assistants without requiring the AI to hallucinate or manage internal database identifiers (UUIDs) or timestamps. The application automatically generates UUIDs, timestamps, and stub cards for any referenced targets.

### 1. Single Card Schema

When prompting an AI to generate or summarize a card, use the following clean JSON schema:

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

You can also export or paste an array of cards:

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

### AI Workflow Features

- Copy Card JSON: Tap the code icon (`</>`) on any card's detail screen to view and copy its clean JSON representation to the clipboard.
- Paste JSON Ingestion: Navigate to the Backup screen and tap "Paste JSON" to paste AI-generated JSON directly into LinkView.
- Mention Syntax: Mentions in context units use the `@` or `@[Target Title]` format. If the target card does not yet exist, LinkView automatically creates a new stub card for it.

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
- feature:detail: Card detail view with styled section chips for Context and Extra Context, Markdown rendering, and JSON export.
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
