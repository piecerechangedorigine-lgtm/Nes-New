# Nova — Technical Debt

Every item below was verified against the actual code, not assumed.
Items resolved by Phase 8.5 are kept (struck through, not deleted) so
this doc stays a reliable history — a future contributor shouldn't have
to guess whether something was fixed or just forgotten about.

---

## ✅ Resolved in Phase 8.5

- ~~No edit or delete UI for Accounts, Transactions, or Budgets~~ —
  fixed (8.5.1). Every entity has full CRUD now.
- ~~Profile toggles don't survive process death~~ — fixed (8.5.4),
  DataStore-backed.
- ~~`NovaUseCase` (non-flow base class) is dead code~~ — fixed.
  `ExportBackupUseCase`/`ImportBackupUseCase` (8.5.8) are the first
  concrete use cases built on it.
- ~~Budgets and Goals screens silently swallow load errors~~ — found
  and fixed during the 8.5.10 stability pass. Both tracked an
  `errorMessage` in their `UiState` but never rendered it.

---

## 🔴 Correctness gaps (matter most)

### 1. Account balance is still disconnected from the transaction ledger

**Not fixed by Phase 8.5 — still the single biggest correctness gap.**
`FinancialSource.currentBalance` and `.availableBalance` are both
still static numbers set at creation/edit time. Adding, editing, or
deleting a transaction does **not** update the balance of the source
it belongs to — confirmed by re-reading `TransactionRepositoryImpl`
and every transaction call site again this phase; nothing has changed
here since Phase 8's writeup.

This is not incidental — `GetBalanceOverviewUseCase` (new in 8.5.3)
makes the gap more visible, not less: Total Liquidity and Available
Spending Power are both computed from the same static
`currentBalance` figures, so they're exactly as disconnected from real
transaction activity as Dashboard's total always was. The two
legitimate fixes described in Phase 8's writeup (auto-adjust on every
transaction write, or make manual reconciliation explicit in the UI
with a "last updated" indicator) are both still open, still need a
product decision, and are now the clear #1 item for whoever picks up
Phase 9 or a Phase 8.6.

### 2. Edit/delete now exists — but account balance validation is thin

New in 8.5.1: `AccountFormSheet` validates that the balance field
parses to a number, but doesn't validate anything domain-specific
(e.g. no floor/ceiling, no confirmation on a large balance swing
beyond the informational "balance will change by X" hint it already
shows). Low severity — worth a look if editing accounts turns out to
be error-prone in practice, not urgent today.

---

## 🟡 New foundations that are real but not fully wired

Phase 8.5's engines and infrastructure are genuinely implemented and
tested — not stubs — but several have no UI surface yet. Listed here
so nobody assumes "exists in `core:domain`" means "visible to the
person using the app":

### 3. `GetBalanceOverviewUseCase` has zero UI wiring

Total Liquidity, Available Spending Power, and Dream Safe Balance
(8.5.3) are computed, correct, and covered by 5 passing tests — and
displayed nowhere. No screen injects this use case. The Dashboard
still shows its own simpler `totalBalance` (sum of active sources,
liabilities included as if they were assets — see item #1's sibling
concern). Wiring `GetBalanceOverviewUseCase` into the Dashboard or a
dedicated balance breakdown screen is a clean, contained next step.

### 4. `NovaSoundManager` isn't called from any screen

The manager (8.5.9) is real: haptic feedback is genuinely wired to
`SoundMode`, and the sound-asset hook is a documented, honest no-op
rather than a fake beep. But grep confirms it: nothing outside the
manager's own file and `ProfileSettings`'s doc comment references
`rememberNovaSoundManager` or calls any of its methods. No delete
confirmation, no successful form submission, nothing triggers haptic
feedback yet. The plumbing is done; the last mile (wiring it into
`NovaConfirmDialog` and the four CRUD forms) isn't.

### 5. `BalanceUpdateMode.ASSISTED` and `.SMART` are inert

Every `FinancialSource` created today gets `MANUAL` and nothing
branches on the other two values — by design (see `ROADMAP_NEXT.md`),
but worth stating plainly: these are enum values with zero behavior,
not partially-implemented features.

### 6. Permission Center: 5 of 6 permissions aren't real OS permissions

Documented clearly in `PermissionType`'s own doc comment and repeated
here for visibility: only `NOTIFICATIONS` maps to an actual Android
runtime permission. SMS, Camera, OCR, Widgets, and Background Sync all
track in-app "acknowledged" state in DataStore, not a real grant —
because the features they'd back don't exist yet, and this app doesn't
request permissions with nothing behind them. Tapping "Learn more" on
any of the five doesn't open a system dialog.

---

## 🟢 Testing gaps

### 7. Test coverage still stops at `core:domain`

61 unit tests now (up from 43), all pure-JVM. Same structural gap as
Phase 8: no ViewModel tests, no Compose UI tests, no Room instrumented
tests. Phase 8.5 makes this gap slightly more pointed in one place:

### 8. `BackupRepositoryImpl` has no test coverage at all

The pure-Kotlin part of Backup & Restore (`BackupPayload`'s JSON
round-trip and version validation) has 4 real tests. The part that
actually matters most for data safety — `restoreFromPayload()`'s
atomic wipe-and-rewrite transaction — has none, because it needs a
real or in-memory Room database that a pure JVM test can't provide.
This is the single highest-value instrumented test to write first if
anyone picks up the "no Room instrumented tests" item below: a restore
that fails halfway through and either fully commits or fully rolls
back is exactly the kind of behavior that's easy to get wrong silently
and expensive to get wrong in production.

### 9. No CI

Still no GitHub Actions (or any other CI) workflow. 61 tests, still
only run when someone remembers to type `./gradlew test`.

### 10. Room schema export directory is still gitignored

Unchanged from Phase 8 — `room.schemaLocation` exports on every build,
`core/data/schemas/` is gitignored, so the export happens and is
immediately thrown away. `MIGRATION_2_3` (this phase's `accounts` →
`financial_sources` rename) would have been a good candidate for a
real `MigrationTestHelper` test verifying the migration against an
exported v2 schema — another reason to resolve this inconsistency
rather than let it compound with every future migration.

---

## 🔵 Scale / performance debt (not urgent, still real)

### 11. Analytics, Assistant, and now Forecast all recompute over the full ledger

`GetForecastSummaryUseCase` (new, 8.5.6) is built directly on
`GetDashboardSummaryUseCase`, which already recomputes from the full
transaction list on every change — so the forecast inherits the same
"fine at realistic personal-finance scale, no pagination or
pre-aggregation" characteristic Analytics and the Assistant already
had. Not a new problem, just a new consumer of the existing one.

### 12. No pagination on transaction lists

Unchanged from Phase 8.

### 13. `NovaUseCase` non-flow base class — no longer dead code, still worth a note

Now used by exactly two use cases (`ExportBackupUseCase`,
`ImportBackupUseCase`). Not debt exactly, but worth flagging that it's
a thin base class validated by only one feature so far — if a second,
different single-shot use case reveals the shape doesn't generalize
well, that's the first sign to watch for.

---

## ⚪ Smaller / latent items

### 14. Still no injectable `Clock` — now affects two more calculations

`GetForecastSummaryUseCase` and `GoalForecast.health()` both default
to `LocalDate.now()` the same way `GetDashboardSummaryUseCase` and
`SavingsGoal.forecast()` already did. `GoalForecast.health()` at least
accepts `today` as a parameter (matching `forecast()`'s existing
pattern), which is what made it possible to test at all — see
`GoalHealthTest`. `GetForecastSummaryUseCase`'s day-of-month math is
private and not parameterized, so its tests (like
`GetDashboardSummaryUseCaseTest` before it) had to be written around
wide, day-independent margins rather than testing exact projected
figures. Still no `Clock` abstraction anywhere in the codebase.

### 15. `Money.formatted()` still silently defaults to USD — now with a persisted currency setting that doesn't feed it

`ProfileSettings.currency` is real and persists (8.5.4) — but nothing
reads it when formatting a `Money` value. Every `.formatted()` call
site across every screen still uses the USD default. This is a more
pointed version of the Phase 8 item: the currency preference now
*exists* and *looks* wired to a person picking their currency, but
isn't actually connected to a single rendered number anywhere.

### 16. Goal Health's "pace" component is a proxy, not real contribution history

Documented in `GoalHealth.kt`'s own doc comment and repeated here:
`GoalRepository.contribute()` only ever increments a running total —
there's no per-contribution timestamp log. The pace component
approximates "contribution consistency" via percentComplete vs.
elapsed-time-ratio, which rewards steady overall progress but can't
actually detect whether someone contributed once in a lump sum or
consistently every week. A real consistency measure needs a
`GoalContribution` history table this schema doesn't have.

### 17. No accessibility audit performed

Unchanged from Phase 8.

### 18. Benchmark numbers aren't enforced anywhere

Unchanged from Phase 8.

---

## Explicit non-issues (checked, found fine)

- **Enum storage in Room** — still by `.name`, not `.ordinal`, across
  every enum column including the two new ones (`FinancialSourceType`,
  `BalanceUpdateMode`).
- **Migration safety** — `MIGRATION_2_3` re-verified: no
  `RENAME COLUMN`, no destructive fallback, old `AccountType` values
  with no direct new-enum equivalent (`INVESTMENT`) are explicitly
  remapped rather than left to fail `valueOf()` on next read.
- **Backup atomicity** — `restoreFromPayload()` wraps the full
  wipe-and-rewrite in one `database.withTransaction {}`. A restore
  either fully succeeds or leaves existing data completely untouched.
- **Permissions** — `POST_NOTIFICATIONS` is the only new
  `<uses-permission>` entry, and it backs a real, working feature
  (Profile's notification toggle). No permission was added for a
  feature that doesn't exist.

---

## Added in Phase 9 (Dream Dashboard)

Not a full re-audit — Phase 9's own completion report and
`DASHBOARD_ARCHITECTURE.md` cover that phase in depth. Two items
surfaced there worth tracking here too:

- **`WidgetSize` (SMALL/MEDIUM/LARGE) persists and is fully editable,
  but most widgets render the same layout regardless of the value.**
  The data model and Dashboard Studio's UI control both work
  end-to-end; building genuinely distinct visual densities per
  (widget type × size) combination is real UI work that didn't happen
  this phase. See `DASHBOARD_ARCHITECTURE.md`'s "Why 9.1's resize is
  real but incomplete."
- **No test coverage for anything Compose-rendered in Phase 9** — the
  five goal visualization modes, the four widget cards, and all three
  Glance home screen widgets are untested, for the same structural
  reason every other Compose UI in this app is untested (see "Testing
  gaps" above), plus Glance widgets specifically are hard to unit test
  at all (`GlanceAppWidgetReceiver`/`provideGlance` need real Android
  framework classes).
