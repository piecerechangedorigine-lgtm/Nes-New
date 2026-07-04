# Nova — Architecture

Multi-module Android app, Kotlin + Jetpack Compose, MVVM, Hilt DI,
Room persistence. Everything below reflects what's actually in the
repository, not an aspirational target.

---

## Module graph

```
                    ┌─────────┐
                    │  :app   │
                    └────┬────┘
          ┌──────────────┼──────────────────────────┐
          │               │                          │
   ┌──────▼──────┐ ┌─────▼──────┐          ┌────────▼────────┐
   │ :core:data  │ │:core:nav-  │          │ :feature:*       │
   │             │ │ igation    │          │ dashboard        │
   └──────┬──────┘ └────────────┘          │ accounts         │
          │                                 │ transactions     │
   ┌──────▼──────┐  ┌───────────────┐       │ budgets          │
   │:core:domain │◄─┤:core:design-  │◄──────┤ goals            │
   │             │  │ system        │       │ analytics        │
   └─────────────┘  └───────────────┘       │ assistant        │
                                             │ profile          │
                                             └──────────────────┘

   :benchmark → instruments :app (Macrobenchmark, separate build type)
```

**Dependency direction is one-way and enforced by what each module
actually imports, not just convention:**

- `core:domain` depends on nothing else in this project. Pure Kotlin,
  zero Android framework imports, zero Compose imports. This is why
  its use cases and models are unit-testable with plain JUnit.
- `core:data` depends on `core:domain` only. Implements the repository
  interfaces `core:domain` declares.
- `core:designsystem` depends on `core:domain` (for `TransactionCategory`
  color mapping — see `NovaCategoryColor.kt`) and Compose, but never on
  `core:data` or any feature module.
- `core:navigation` depends on nothing but Compose Navigation — it only
  holds the `NovaNavGraphProvider` interface and route string constants,
  no feature knowledge.
- Every `feature:*` module depends on `core:domain`, `core:designsystem`,
  `core:navigation` — never on another `feature:*` module directly, and
  never on `core:data` (features only ever see repository interfaces
  through injected use cases).
- `:app` depends on everything, but contains almost no logic of its own
  — theme + Scaffold + collecting the `Set<NovaNavGraphProvider>` Hilt
  assembles from every feature module.

If a change ever needs a `feature:*` module to import another
`feature:*` module directly, that's a sign the shared piece belongs in
`core:domain` or `core:designsystem` instead.

---

## Layer responsibilities

### `core:domain` — business logic, framework-free

- **Models** (`Money`, `Transaction`, `FinancialSource`, `Budget`,
  `SavingsGoal`, `DashboardSummary`, `AnalyticsSummary`,
  `AssistantContext`, `BalanceOverview`, `ForecastSummary`,
  `GoalHealth`, `ProfileSettings`, `PermissionInfo`, `BackupPayload`,
  ...) — plain data classes. `Money` is a `@JvmInline value class`
  wrapping integer minor units (cents), never a raw `Double`,
  specifically to avoid floating-point drift in financial arithmetic.
  `FinancialSource` (renamed/expanded from `Account` in Phase 8.5) is
  the general "anywhere money lives" abstraction — bank account, cash,
  credit card, e-wallet — replacing the original salary-centric model.
- **Repository interfaces** — the only contract `core:data` has to honor.
  Domain code (and tests) never knows Room or DataStore exists.
- **Use cases** — one piece of business logic each, built on two base
  classes in `NovaUseCase.kt`:
  - `NovaUseCase<Params, Result>` — single-shot suspend operation,
    wraps the result in `NovaResult` and catches exceptions into
    `NovaFailure.Unknown`. Sat unused through Phase 7; Phase 8.5's
    `ExportBackupUseCase`/`ImportBackupUseCase` are the first concrete
    use cases built on it, since export/import are exactly the
    one-shot shape it was designed for.
  - `NovaFlowUseCase<Params, Result>` — used everywhere else. Wraps a
    `Flow<NovaResult<Result>>`, confines it to a given
    `CoroutineDispatcher` via `flowOn`, and maps any thrown exception
    into `NovaResult.Error` instead of letting it propagate into a
    ViewModel's collector.
- **`AssistantInsightEngine`** — deliberately rule-based, not an LLM
  call. See the "Assistant" section below.
- **`GoalForecast.health()`** (Phase 8.5.7) and
  **`GetForecastSummaryUseCase`** (Phase 8.5.6) — both deliberately
  simple, explainable scoring/projection, matching the same philosophy
  as the Dashboard insight banner and the Assistant. Neither is a
  trend-fit model.
- **`BackupPayload` + `toJson()`/`parseBackupPayload()`** (Phase
  8.5.8) — dedicated DTOs with their own `CURRENT_BACKUP_VERSION`,
  deliberately decoupled from the domain models themselves so a future
  model refactor can't silently break an old backup file's schema.
- **`DashboardLayout`, `DashboardWidgetConfig`, `DashboardPreset`,
  `GoalVisualizationMode`, `DreamBackground`, `DreamDashboardData`**
  (Phase 9) — the Dream Dashboard's full domain model. See the
  dedicated `DASHBOARD_ARCHITECTURE.md` for how these fit together;
  the short version is that `GetDreamDashboardDataUseCase` composes
  every financial use case a widget could need into one call, and
  Phase 9 built the configurable widget system around that data
  without inventing any new financial calculation.
- **`Debt`, `DebtHealthScore`, `DebtWeather`, `PayoffPlanResult`,
  `DebtPressureScore`, `DebtDreamImpact`, `DebtSummary`** (Phase 10) —
  the Debt Intelligence Center's full domain model, a deliberately
  separate entity from `FinancialSource` rather than an extension of
  it. See the dedicated `DEBT_ARCHITECTURE.md` for the full reasoning,
  including why that separation creates a real reconciliation gap this
  phase didn't close.
- **`CreditCardUtilization`, `SourceHealth`, `BalanceHealthScore`,
  `SourceForecast`, `BalanceSuggestion`, `SourceGroup`,
  `BalanceSnapshot`, `FinancialSourceIntelligence`** (Phase 11) — five
  new intelligence engines over `FinancialSource`, none of them
  duplicating a calculation that already existed elsewhere (Balance
  Health directly reads Debt Health and Forecast rather than
  recomputing either). See `FINANCIAL_SOURCES_ARCHITECTURE.md` for the
  full reasoning, including why `BalanceOverview` itself — Phase 11's
  actual "Balance Intelligence Engine" — kept its Phase 8.5 name.
- **`ReconciliationConflict`, `effectiveLiabilityBalance`,
  `effectiveCreditCardUtilization`, `SourceAnalytics`,
  `WidgetCatalog`** (Phase 11.5) — the reconciliation layer that closes
  the FinancialSource/Debt double-counting gap flagged since Phase 10,
  plus Source Analytics' domain layer and the Dashboard widget-add
  catalog. See `FINANCIAL_SOURCES_ARCHITECTURE.md` and
  `FOUNDATION_COMPLETION_REPORT.md`.

### `core:data` — persistence

- Room database (`NovaDatabase`, version 6 as of Phase 11.5.1's
  `linkedDebtId` reconciliation column — version 5 was Phase 11's
  credit card + inclusion-control columns and the new
  `balance_snapshots` table, version 4 was Phase 10's new `debts`
  table, version 3 was Phase 8.5's `FinancialSource` rename), one
  `Entity`/`Dao` pair per feature area, `Converters` for the enum
  columns (`FinancialSourceType`, `BalanceUpdateMode`,
  `TransactionCategory`, `DebtDirection`, `DebtType` — all stored by
  `.name`, never `.ordinal`, so reordering enum entries can't silently
  corrupt existing rows).
- Repository implementations map `Entity ↔ domain model` and nothing
  else — no business logic lives here. `BackupRepositoryImpl` is the
  one deliberate exception: it reads/writes DAOs directly rather than
  through the per-feature repositories, since a full-database
  snapshot/restore is a genuinely cross-cutting operation those
  repositories' scoped interfaces (e.g. `BudgetRepository` is
  deliberately month-scoped) shouldn't have to support.
- `DataStoreModule` provides a single named `DataStore<Preferences>`
  instance (`ProfileRepositoryImpl`, `PermissionRepositoryImpl`,
  `DashboardRepositoryImpl` — Phase 9 — and, as of Phase 11,
  `SourceGroupRepositoryImpl` — each read/write it under their own key
  namespace).
- `DatabaseModule` wires Room with **no destructive fallback**: a
  missing `Migration` fails the build loudly rather than silently
  wiping user data. `MIGRATION_1_2` (Phase 7, index-only) and
  `MIGRATION_2_3` (Phase 8.5, the `accounts` → `financial_sources`
  rename + new columns) are the two migrations that exist so far —
  both built from `ADD COLUMN`/`RENAME TABLE`/`UPDATE` only, no
  `RENAME COLUMN`, since that statement isn't supported on the SQLite
  version bundled with early Android 8.x devices at this app's minSdk.
- `DispatcherModule` provides a single unqualified `CoroutineDispatcher`
  (`Dispatchers.Default`) for use cases, since every current use case
  does CPU-bound aggregation over flows Room already delivers off the
  main thread.

### `core:designsystem` — the `Nova` token system

Everything reads through the `Nova` object (`Nova.colors`, `.typography`,
`.spacing`, `.shapes`, `.elevation`) rather than raw values or even
`MaterialTheme` directly, except where a stock Material3 component
(`Button`, `Scaffold`, `OutlinedTextField`) needs `MaterialTheme` to be
populated for its own internal styling — `NovaTheme` provides both in
parallel.

- Dark-only theme (`NovaDarkColors` is the only palette; `darkTheme`
  param on `NovaTheme` exists but has nothing to switch to yet).
- Custom icon set (`NovaIcons`) — hand-drawn monoline `ImageVector`s
  built from straight lines and arcs only, matching the logomark's
  construction, rather than pulling from Material Icons.
- Custom chart primitives (`NovaDonutChart`, `NovaGroupedBarChart`,
  `NovaLineChart`) — the bar chart is built from `fillMaxHeight(fraction)`
  boxes rather than `Canvas`, since flat-topped bars with no curves
  don't need pixel-level drawing; the donut and line charts use `Canvas`
  where actual arcs/paths are unavoidable.
- `NovaCategoryColor.kt` — caches `TransactionCategory → Color` so
  every transaction row, budget card, and analytics swatch parses each
  category's hex string exactly once per process, not once per
  recomposition (Phase 7).
- `NovaConfirmDialog` (Phase 8.5.1) — the one delete-confirmation
  dialog every CRUD feature (Accounts, Transactions, Budgets, Goals)
  shares, instead of four bespoke `AlertDialog`s with slightly
  different copy and behavior.
- `NovaSoundManager` / `rememberNovaSoundManager()` (Phase 8.5.9) —
  real haptic feedback wired to `SoundMode`; sound playback is a
  documented no-op placeholder until real audio assets exist (see the
  file's own doc comment — no placeholder beep ships in its place).

### `core:navigation`

Just `NovaNavGraphProvider` (the interface every feature implements)
and `NovaRoutes` (route string constants, including route-pattern
builders like `NovaRoutes.transactions(accountId)`). Cross-feature
navigation intent lives here specifically so neither feature module has
to depend on the other to know its route. A feature module can
register more than one composable in `registerGraph` — `feature:profile`
does this as of Phase 8.5.5, registering both `NovaRoutes.PROFILE` and
the new `NovaRoutes.PERMISSIONS` (Permission Center) from the same
`ProfileNavGraphProvider`, since the two screens are closely related
and splitting Permissions into its own feature module for one screen
wasn't worth the Gradle module overhead.

### `feature:*` — one module per screen area

Consistent internal shape in every feature module:

```
feature/<name>/
  <Name>Screen.kt        — Route composable (hoists ViewModel) + stateless Screen composable
  <Name>ViewModel.kt      — @HiltViewModel, exposes StateFlow<XUiState>
  navigation/
    <Name>NavGraphProvider.kt  — registers this feature's composable(s) into the shared NavHost
    <Name>NavModule.kt         — Hilt @Binds @IntoSet, contributes the provider to Set<NovaNavGraphProvider>
```

Every `XUiState` data class is annotated `@Immutable` (Phase 7) — since
they all carry `List<T>`, which the Compose compiler treats as
unstable by default, the annotation is what lets Compose actually skip
recomposition when the state reference hasn't changed.

### `:app`

Single Activity (`MainActivity`), Hilt-injects the full
`Set<NovaNavGraphProvider>` and builds the `NavHost` by delegating to
each one — `NovaNavHost` never references a feature composable by name.
Bottom nav shows only on the 4 top-level destinations (`NovaDestination.
bottomNavDestinations` — Dashboard, Accounts, Analytics, Profile);
everything else (Transactions, Budgets, Goals, Assistant, Dashboard
Studio, Permission Center) is reached by navigating there from one of
those four, keeping the nav tree shallow per the original "max 4
primary destinations" UX rule.

`app/widget/` (Phase 9.9) is the one place `:app` contains real logic
beyond bootstrapping — three Glance (`GlanceAppWidget`) home screen
widgets plus `WidgetEntryPoint`, the Hilt `@EntryPoint` they use to
reach the domain layer from outside Hilt's normal construction path
(Glance widgets are instantiated by the Android system via
`GlanceAppWidgetReceiver`, not by Hilt). See `DASHBOARD_ARCHITECTURE.md`
for the full writeup, including why there's no separate "lock screen
widget" code path (9.10) — Android has no public API for one.

`feature:debt` (Phase 10) follows the same per-feature module shape
every other feature module already does — its own `ViewModel`/`Screen`
pairs, its own `NavGraphProvider`/`NavModule` pair (registering both
the main Debt Intelligence Center screen and the Debt Simulator screen
from one provider, the same multi-route pattern `feature:profile`
already established for Profile + Permissions). See
`DEBT_ARCHITECTURE.md` for what it's built on.

`app/worker/` (Phase 11.5.3) gives `WorkManager` — a dependency this
project has declared since Phase 1 without a single real consumer —
its first two: `BalanceSnapshotWorker` and `FinancialHealthCheckWorker`,
both `@HiltWorker`-annotated so they get the same constructor injection
every ViewModel already gets, via `HiltWorkerFactory` wired in
`NovaApplication`'s `Configuration.Provider` implementation.
`WorkScheduler` owns all scheduling policy (frequency, constraints,
retry/backoff, unique work naming) separately from what each worker
actually does — see `FINANCIAL_SOURCES_ARCHITECTURE.md` for the
battery-conscious reasoning behind the specific choices made there.
`app/notification/` is the one small helper both workers share for
posting to a single notification channel, checking the real runtime
`POST_NOTIFICATIONS` grant before ever calling `notify()`.

### CI/CD

`.github/workflows/ci.yml` runs on every push to `main` and every pull
request: compile, unit tests, Android Lint, and Detekt, in that order,
each one failing the whole workflow on any real finding.
`.github/workflows/release.yml` runs the identical gate on a version
tag before building an unsigned release artifact and drafting a GitHub
Release — a tag pointing at code that wouldn't pass CI never produces
a release artifact. `config/detekt/detekt.yml` documents, rule by
rule, which Detekt defaults were deliberately loosened for this
codebase's actual conventions (prose-dense doc comments, composed
use cases with legitimately many dependencies) and why — see that
file's own header. Both workflows invoke the committed `./gradlew`
wrapper (`gradlew`/`gradlew.bat` — standard, unmodified Gradle
scripts). **`gradle/wrapper/gradle-wrapper.jar` itself is still
missing** — see `gradle/wrapper/MISSING_WRAPPER_JAR.md` for exactly
why (no network access and no local Gradle install were available to
generate it) and the one-command fix (`gradle wrapper --gradle-version
8.10`) needed to complete this from an environment that has either.

### `:benchmark`

`com.android.test` module, targets `:app`'s `benchmark` build type
(release-shaped: minified, non-debuggable, but debug-signed so it can
be instrumented without the real release key). `StartupBenchmark.kt`
measures cold-start via Macrobenchmark; see `RELEASE.md` for why no
generated Baseline Profile ships yet.

---

## Key patterns

### Single source of truth via composed use cases

Dashboard, Analytics, Budgets, Goals, and the Assistant all read
*through* use cases that themselves compose from the same underlying
repository flows — never a second, parallel aggregation of the same
data. `GetAssistantContextUseCase` is the clearest example: it
`combine()`s the exact same `GetDashboardSummaryUseCase`,
`GetAnalyticsSummaryUseCase`, `GetBudgetProgressUseCase`, and
`GetGoalForecastUseCase` that power their own screens, rather than
re-querying repositories directly. This is what makes it structurally
impossible for the Assistant to tell you a different spending number
than the Analytics screen shows.

### `NovaResult` instead of exceptions crossing layers

Every use case returns `NovaResult<T>` (`Success` / `Error` / `Loading`)
rather than throwing. ViewModels pattern-match on it and never wrap use
case calls in `try/catch`. `NovaFailure` is a closed sealed class so the
UI layer can branch on specific failure kinds rather than showing a
generic "something went wrong."

### Assistant: rule-based by design, not by accident

`AssistantInsightEngine` is deliberately **not** an LLM call. Nova has
no backend of its own, and a real model integration would need either
a client-embedded API key (a hard no) or a server-side proxy (out of
scope for this module). The engine's `respond(query, context)` contract
is the intentional swap-seam: a future network-backed implementation
can replace it without touching `AssistantViewModel` or the screen.
Every current reply branch is an explainable, checkable fact — the same
"deliberately simple heuristics, not a model" approach
`GetDashboardSummaryUseCase`'s own insight banner uses.

### Deterministic IDs where uniqueness is structural

`BudgetsViewModel` builds budget IDs as `"${category.name}_$month"`
rather than a random UUID — one budget per category per month is a
structural invariant, so the ID itself enforces it (re-submitting the
same category upserts instead of creating a silent duplicate). Phase 7
backed this up with a real DB-level unique index
(`(month, category)` on `BudgetEntity`) rather than relying on the
application layer alone.

### Compose stability is treated as a real correctness/perf concern

Every `UiState` class is `@Immutable`. Every category color and every
formatted currency string is cached rather than recomputed per
recomposition (Phase 7 — see `NovaCategoryColor.kt` and the formatter
cache in `Money.kt`). Every `LazyColumn` uses a stable `key = { it.id }`.

### Destructive actions are atomic or they don't happen at all

`BackupRepositoryImpl.restoreFromPayload()` wraps the full wipe +
rewrite of every table in a single `database.withTransaction {}` block
(Phase 8.5.8) — a restore that failed halfway through would otherwise
leave foreign-key references between transactions and financial
sources dangling. Same principle, smaller scale, in every CRUD delete
flow: `NovaConfirmDialog` is the one gate every delete (account,
transaction, budget, goal) goes through, so there's exactly one place
that decision gets made consistently rather than four.

---

## Build system

- Gradle version catalog (`gradle/libs.versions.toml`) — every version
  and every plugin ID is declared once, referenced by alias everywhere.
- Standard multi-module Android setup: `com.android.application` for
  `:app`, `com.android.library` for every `core:*`/`feature:*` module,
  `com.android.test` for `:benchmark`.
- Hilt + KSP for DI codegen, Room + KSP for persistence codegen,
  `kotlinx.serialization` (Phase 8.5.8, `core:domain` only) for the
  Backup JSON payload and (Phase 9) the Dashboard layout JSON. Coil
  (`core:designsystem`-adjacent, `feature:dashboard`) loads the Dream
  Background image; Glance AppWidget (`:app` only) renders the three
  home screen widgets.
- Release signing reads from a gitignored `app/keystore.properties`
  (template at `app/keystore.properties.example`), falls back to an
  unsigned build when absent so the project stays buildable without
  the real release key.
