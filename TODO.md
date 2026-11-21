# LinkView Todo and Roadmap

## Planned Features

### In-App SQLite Query Console

An interactive SQL query console for advanced users to inspect and query the local SQLite database (`linkview.db`) directly from within the application.

#### 1. Entry Point and Navigation
- Add a terminal/database icon next to the Backup & Portability icon in the top header.
- Tapping the icon opens the SQLite Console screen (or modal drawer on tablets).

#### 2. Schema Reference Sheet
- Collapsible side panel or drawer showing the full relational schema:
  - `cards`: `id` (TEXT PK), `title` (TEXT NOCASE), `type` (TEXT), `tagsCsv` (TEXT), `createdAt` (INTEGER), `updatedAt` (INTEGER)
  - `context_units`: `id` (TEXT PK), `source_card_id` (TEXT FK), `sort_order` (INTEGER), `raw_text` (TEXT), `plain_text` (TEXT)
  - `unit_references`: `unit_id` (TEXT FK), `target_card_id` (TEXT FK)
  - `card_tags`: `card_id` (TEXT FK), `tag` (TEXT)
- Tapping any table or column name automatically inserts it into the query editor at the cursor position.
- Pre-baked template queries for common analytical checks:
  - Cards with the highest incoming backlink counts
  - Finding orphaned or stub cards with no context units
  - Tag frequency and co-occurrence distributions
  - Reverse backlink graph traversals

#### 3. Query Editor and Execution
- Monospaced multi-line text input with syntax-friendly font and line numbers.
- Execute button measuring execution duration (e.g. `24 rows returned in 1.4 ms`).
- Graceful error banner capturing `SQLiteException` with syntax error positions without crashing the application.

#### 4. Tabular Results Display
- Adaptive data table dynamically displaying column headers and cell values from the `Cursor`.
- Horizontal and vertical scrolling for wide result sets.
- Row count indicator and export/copy results as CSV or JSON.
