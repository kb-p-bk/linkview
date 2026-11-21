# LinkView

LinkView is an offline-first, bidirectional linked note-taking application designed for Android tablets and phones. It is built natively with Jetpack Compose, Kotlin Coroutines, and Room SQLite.

## Overview

Traditional note-taking applications organize information into long, monolithic documents or strict hierarchical folders. LinkView organizes knowledge into atomic Cards containing modular Context Units. Every mention between cards creates an automatic bidirectional reference, splitting context into:

- Context: Authored notes written directly on the current card.
- Extra Context: Incoming backlinks from all other cards that mention this card.

## Core Features

- Atomic Cards and Context Units: Each card represents a discrete topic, person, place, or concept, composed of reorderable context units.
- Bidirectional Mentions: Mention other cards using @[Card Title] syntax with autocomplete suggestions.
- Intra and Extra Context: View notes authored on the card alongside references coming from other cards.
- Multi-Tag Filtering: Assign multiple tags to cards and filter using relational AND-queries.
- Tablet-Optimized Split View: Responsive layout featuring a full-width search header and a 38-62 proportional split view between the card grid and card content.
- Offline-First and Private: All data is stored locally in an encrypted/private SQLite database using Room. No account or network connection is required.
- JSON Backup and Restore: Export and import complete card collections, tags, and relational references with schema validation.

## Architecture

The project follows Clean Architecture and Android modularization best practices:

- app: Application shell, dependency injection setup, and top-level responsive navigation.
- core:model: Domain entities (Card, ContextUnit, CardWithContext, SearchResult).
- core:common: Utilities, mention tokenizers, and common dispatchers.
- core:ui: Common Compose design system components, typography, and theme.
- domain: Repositories and business logic use cases.
- data:database: Room database, entities, DAOs, and relational queries.
- data:datastore: User preferences and search scope settings via Jetpack DataStore.
- data:repository: Implementations for card and search repositories.
- data:backup: JSON backup export and import parser with transaction support.
- feature:cards: Card grid screen, full-width search header, and tag autocomplete.
- feature:detail: Card detail view with styled section chips for Context and Extra Context.
- feature:editor: Card editor, multi-tag input, context unit management, and debounced @mention autocomplete.
- feature:backup: Backup creation and restoration user interface.

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
