# Nova — Foundation Completion Report (Phase 11.5)

Tagged v1.3.5-foundation-complete. This report covers the full
architecture audit (11.5.6), what closed and what didn't across the
rest of Phase 11.5's scope, and an honest readiness assessment before
Phase 12 (Automation Foundation) begins.

---

## 1. Architecture Audit (11.5.6) — methodology and findings

Every check below was run against the actual repository, not asserted
from memory. Where a script or grep pattern was used, the pattern and
the real output are both stated, so the finding is verifiable rather
than a claim.

### 1.1 Duplicate declarations

Scanned every `.kt` file for duplicate top-level `class`/`fun`/`object`/
`interface`/`enum class`/`sealed class` declarations.

**Finding: zero real duplicates.** One flagged match
(`core/domain/.../Money.kt` reporting `fun Money` twice) is a false
positive — it's two distinct extension functions sharing a receiver
name (`fun Money.formatted()` and `fun Money.formattedWithSign()`),
not two conflicting declarations of the same symbol.

### 1.2 Dead repositories

Checked that every repository interface in `core:domain/repository`
has exactly one implementation in `core:data`, and that every
implementation is bound in `RepositoryModule`.

**Finding: all 11 repositories** (`FinancialSourceRepository`,
`TransactionRepository`, `BudgetRepository`, `GoalRepository`,
`DebtRepository`, `ProfileRepository`, `PermissionRepository`,
`BackupRepository`, `DashboardRepository`, `BalanceSnapshotRepository`,
`SourceGroupRepository`) have exactly one implementation, and all 11
are bound. No dead repository interfaces, no unbound implementations.

### 1.3 Unused use cases

Checked every class in `core:domain/usecase` for at least one
reference outside its own file (excluding tests).

**Finding: zero real unused use cases.** One flagged file
(`BackupUseCases.kt`) is a false positive — the grep matched on the
*filename*, but the file contains two classes (`ExportBackupUseCase`,
`ImportBackupUseCase`) with different names, both of which are
genuinely consumed in `ProfileViewModel.kt`.

### 1.4 Orphaned navigation destinations

Checked every `NovaRoutes` constant for both a registered `composable()`
entry and at least one real `navController.navigate()` call site.

**Finding: all 12 routes are registered and reachable.** One
false-positive flag (`TRANSACTIONS`) was actually reached exclusively
through the `NovaRoutes.transactions(accountId)` function form rather
than the bare constant — a grep artifact, not a real gap; verified by
direct inspection of the four call sites.

### 1.5 Duplicate widget definitions

Checked that every `DashboardWidgetType` enum entry has exactly one
branch in `DashboardWidgetDispatcher`'s `when`.

**Finding: exactly one branch per type, for all 11 types** (`GOAL`,
`FORECAST`, `FINANCIAL_OVERVIEW`, `RECENT_ACTIVITY`, `AI_INSIGHTS`,
`DEBT_OVERVIEW`, `DEBT_WEATHER`, `DEBT_COACH`, `DEBT_RECOVERY`,
`SOURCE_HEALTH`, `BALANCE_FORECAST`). No duplicate dispatch, no gaps —
Kotlin's exhaustive-`when` requirement over a closed `enum` is what
actually enforces this at compile time; the audit confirmed the
current state matches that guarantee.

### 1.6 Stale imports

Wrote a heuristic scanner (import statement present, symbol's simple
name never appears again in the file body) across every `.kt` file in
the repository.

**Finding: 29 initial candidates, 24 false positives, 5 real.** The 24
false positives were all `getValue`/`setValue` imports — required by
Kotlin's compiler for property-delegate (`by remember { ... }`)
resolution even though the identifiers never appear as literal text in
the file. The 5 real unused imports (`Spacer`, `LazyColumn`, `items`,
`NovaLinearProgressBar` in `DashboardWidgets.kt`; `MutableStateFlow` in
`DashboardStudioViewModel.kt`; `GetGoalForecastUseCase` in
`DashboardRepositoryImpl.kt`) were removed. The last one is worth
flagging specifically: a data-layer repository importing a domain-layer
use case is backwards regardless of whether it's used — removing it
is an architectural correctness fix, not just cleanup.

### 1.7 Duplicate calculations

Checked every place `core:domain` sums a list of balances for signs of
the same aggregation being computed redundantly in more than one place
for the same purpose.

**Finding: every balance-summing site serves a distinct, already-documented
purpose.** `GetDashboardSummaryUseCase` (a deliberately simple headline
total), `GetBalanceOverviewUseCase` (liability- and reconciliation-aware
net liquidity), `GetDebtSummaryUseCase` (direction-split debt totals),
and `DebtHealth.kt` (health-scoring input) each compute something
genuinely different from the same underlying source data — none of
them is a redundant reimplementation of another.

---

## 2. What closed this phase

- **11.5.1 FinancialSource ↔ Debt Reconciliation** — closed, with real
  tests, real UI (a linking picker in `AccountFormSheet`), and a
  conflict-detection suggestion surfaced through the existing
  `generateBalanceSuggestions` pipeline.
- **11.5.4 Dashboard Widget Management** — closed. `WidgetCatalog` +
  the unified add/restore flow in `DashboardStudioViewModel.onAddWidget`
  replace the `GOAL`-only limitation flagged since Phase 9.
- **11.5.6 Architecture Audit** — performed and documented above, with
  5 real fixes applied.
- **11.5.7 CI/CD Foundation** — closed. `ci.yml` and `release.yml` are
  real, correctly-structured GitHub Actions workflows; `config/detekt/detekt.yml`
  is a genuinely tuned (not blanket-disabled) static analysis config.
- **11.5.8 Documentation Refresh** — all six required documents
  updated, this report created.

## 3. What partially closed

- **11.5.2 Source Analytics Completion** — the domain layer
  (`calculateSourceAnalytics`, `GetSourceAnalyticsUseCase`) is complete
  and tested for all five analytics types. No dedicated screen exists
  yet. This mirrors a real precedent: `GetBalanceOverviewUseCase` spent
  all of Phase 8.5 in exactly this state before Phase 9 gave it a
  widget.
- **11.5.3 WorkManager Activation** — two real, correctly-structured
  workers exist and are scheduled with battery-conscious constraints,
  retry policy, and unique work naming. What's genuinely unverified:
  actual execution on a real device (see Section 5).

## 4. What didn't close

- **11.5.5 Source Groups UI** — not touched this phase. The domain
  model and DataStore repository have existed since Phase 11 with no
  UI in front of them; that remains true.
- **`gradlew` wrapper** — discovered during CI/CD work, not fixed.
  `gradle/wrapper/gradle-wrapper.properties` exists;
  `gradlew`/`gradlew.bat`/`gradle-wrapper.jar` do not. The CI workflows
  route around this via `gradle/actions/setup-gradle` with a pinned
  version, but local development without a generated wrapper is a real
  gap this report is surfacing rather than quietly leaving for someone
  to trip over.

---

## 5. Open risks

1. **WorkManager has never actually run.** Nothing in this development
   environment can execute a background job — no emulator, no device,
   no real Doze-mode scheduler. `BalanceSnapshotWorker` and
   `FinancialHealthCheckWorker` are correctly structured against
   documented AndroidX APIs (Hilt injection, `Configuration.Provider`,
   `PeriodicWorkRequestBuilder`, `Constraints`), but "correctly
   structured" and "verified to actually run and post a real
   notification on a real device" are different claims, and only the
   first one can be made honestly right now.
2. **Reconciliation ownership has no override.** The `Debt` always
   wins over a linked `FinancialSource`'s own balance. If a person's
   actual workflow is the reverse (they maintain the account balance
   carefully and the debt record loosely), there's no way to flip
   that today — see `ROADMAP_NEXT.md`.
3. **CI has never actually run either.** The workflow YAML is
   syntactically real and follows standard, documented GitHub Actions
   patterns, but it has not executed against this repository (no
   GitHub Actions runner available in this environment). The missing
   `gradlew` wrapper in particular is exactly the kind of thing that
   would only surface on a real CI run.

## 6. Technical debt carried forward

Everything already tracked in `TECHNICAL_DEBT.md` (last substantively
updated at Phase 10) remains open — this phase's deliverable list
didn't include updating it, consistent with Phase 10's own precedent
of using phase-specific architecture docs for new debt instead. The
FinancialSource/Debt entity separation itself (two records can still
describe the same real-world thing, now reconciled but not merged) is
worth flagging as ongoing structural debt even though 11.5.1
meaningfully mitigated its worst consequence (silent disagreement).

## 7. Remaining limitations

- No Source Analytics UI, no Source Groups UI (Section 4).
- No `gradlew` wrapper (Section 4).
- `BalanceSnapshotWorker` only records daily, not also weekly, by
  deliberate design (see `FINANCIAL_SOURCES_ARCHITECTURE.md`).
- `FinancialHealthCheckWorker` combines debt reminders and forecast
  alerts into one job rather than the brief's four separate ones, by
  deliberate design.
- No ViewModel or Compose UI test coverage anywhere in this codebase —
  unchanged across every phase, including this one. The new
  `AccountFormSheet` linking UI, `DashboardStudioScreen`'s catalog
  browser, and both new Workers are all untested for this reason.

---

## 8. Readiness assessment

**The platform is meaningfully more internally consistent than it was
at the start of this phase.** The single gap referenced most often
across the last two phases' documentation — FinancialSource/Debt
reconciliation — is genuinely closed, with tests proving the ownership
rule holds across the liquidity, credit utilization, and source health
calculations that depend on it. CI exists for the first time in this
project's history. The Dashboard Studio's widget-management limitation,
flagged since Phase 9, is closed.

**Two things should happen before leaning heavily on this phase's
newest work:**

1. A real device (or emulator) run to confirm `BalanceSnapshotWorker`
   and `FinancialHealthCheckWorker` actually execute, post
   notifications correctly, and behave reasonably under Doze mode —
   this is infrastructure Phase 12's automation work will build directly
   on top of, so its correctness matters more than usual.
2. Generating and committing the `gradlew` wrapper, so the CI
   configuration this phase built can be verified against a real
   GitHub Actions run rather than reasoned about from the YAML alone.

**Recommendation: proceed to Phase 12 (Automation Foundation)**, with
the two verification steps above treated as immediate prerequisites
rather than optional follow-ups — both are cheap to do and both
directly de-risk the foundation Phase 12 is meant to build on.
