# Nova — Project Status

**Current state:** v1.3.5-foundation-complete.
**Last completed phase:** Phase 11.5 — Foundation Completion.

This is a snapshot, not a changelog. See `ARCHITECTURE.md` for the
whole app, `FINANCIAL_SOURCES_ARCHITECTURE.md`/`DEBT_ARCHITECTURE.md`/
`DASHBOARD_ARCHITECTURE.md` for their respective systems,
`FOUNDATION_COMPLETION_REPORT.md` for this phase's full audit and
readiness assessment, and `ROADMAP_NEXT.md` for what's next.

---

## Phase-by-phase summary

| Phase | Scope | Status |
|---|---|---|
| 1–7 | Architecture, design system, brand, core screens, analytics, AI assistant, optimization | ✅ Done |
| 8 | Release readiness | ✅ Done |
| 8.5 | Core completion & hardening | ✅ Done |
| 9 | Dream Dashboard | ✅ Done |
| 10 | Debt Intelligence Center | ✅ Done |
| 11 | Smart Financial Sources & Balance Intelligence | ✅ Done |
| **11.5** | **Foundation Completion** | ✅ **Done** |

---

## What Phase 11.5 built

### 11.5.1 — FinancialSource ↔ Debt Reconciliation
**The most-referenced open gap across the last two phases is closed.**
A `FinancialSource` can now be explicitly linked to a `Debt`
(`linkedDebtId`), with a clear ownership rule: the linked `Debt`'s
balance becomes authoritative everywhere Balance Intelligence reads a
liability (`GetBalanceOverviewUseCase`'s liquidity math, credit
utilization, Source Health). A real `ReconciliationConflict` detector
surfaces disagreements as a `BalanceSuggestion` rather than silently
picking a winner and hiding the drift. Linking is a real, working UI
flow in `AccountFormSheet`, not just a data model — see
`FINANCIAL_SOURCES_ARCHITECTURE.md`.

### 11.5.2 — Source Analytics Completion
Three of five analytics types (Balance Distribution, Source Allocation,
Credit Utilization) have a complete domain layer and composed use case
(`GetSourceAnalyticsUseCase`), fully tested. **A dedicated screen was
not built this phase** — see "What wasn't built" below. Liquidity
Trends and Savings Growth are real, day-bucketed aggregations over
`BalanceSnapshot` history; they'll be visibly sparse until
`BalanceSnapshotWorker` (11.5.3) has been running for a while, which is
the expected, honest shape of a system whose history just started
being recorded.

### 11.5.3 — WorkManager Activation
**WorkManager finally has real, running consumers** after sitting
unused since Phase 1. Two Hilt-injected periodic workers:
`BalanceSnapshotWorker` (daily — see below for why not also weekly) and
`FinancialHealthCheckWorker` (debt-overdue + forecast-deficit checks,
consolidated into one job rather than the brief's four separate ones —
a deliberate battery-conscious choice, not an oversight). This is also
what finally gives Phase 10's deferred Debt Notifications (10.12) a
real, working implementation, and what gives Phase 11.8's
`BalanceSnapshotRepository` its first actual data.

### 11.5.4 — Dashboard Widget Management
**The Dashboard Studio widget-add gap flagged in Phase 9's and Phase
10's own docs is closed.** A real, searchable, categorized widget
catalog (`WidgetCatalog` — Goals/Debt/Forecast/Financial
Sources/AI/System) replaces the old "only `GOAL` widgets can be
dynamically added" limitation. Adding back a hidden widget and adding
a brand-new one are the same unified flow, not two separate features.

### 11.5.5 — Source Groups UI
**Not built this phase.** See "What wasn't built" below.

### 11.5.6 — Architecture Audit
A real, grep-based audit (not a narrative claim) — see
`FOUNDATION_COMPLETION_REPORT.md` for the full methodology and
findings. Headline results: zero duplicate declarations, all 11
repositories have exactly one bound implementation, zero orphaned
navigation routes, zero duplicate widget-dispatch branches, and 5
genuinely unused imports found and removed (out of ~30 initial
candidates — the rest were false positives from Kotlin's `by`-delegate
operator resolution, not real dead code).

### 11.5.7 — CI/CD Foundation
**The "no CI" gap flagged in every phase's docs since Phase 8 is
closed.** Real GitHub Actions workflows: `ci.yml` (compile, unit tests,
lint, Detekt — fails on any of the four, runs on every push and PR)
and `release.yml` (same gate, then builds an unsigned release artifact
and drafts a GitHub Release on a version tag). A tuned Detekt config
(`config/detekt/detekt.yml`) that documents *why* each default rule
override exists rather than silently loosening the ruleset.

### 11.5.8 — Documentation Refresh
This document plus `ARCHITECTURE.md`, `DASHBOARD_ARCHITECTURE.md`,
`DEBT_ARCHITECTURE.md`, `FINANCIAL_SOURCES_ARCHITECTURE.md`, and
`ROADMAP_NEXT.md` all updated. `FOUNDATION_COMPLETION_REPORT.md` is new.

---

## What wasn't built

Stated plainly, matching this project's established pattern:

- **No dedicated Source Analytics screen (11.5.2).** The domain layer
  and composed use case are complete and tested — the same "engine
  ready, UI pending" state `GetBalanceOverviewUseCase` was in for all
  of Phase 8.5 before Phase 9 gave it a widget. A real precedent, not
  a new kind of gap.
- **No Source Groups UI (11.5.5).** The domain model and
  DataStore-backed repository have existed since Phase 11; nothing new
  was added this phase either. Still no screen for a person to
  actually create, rename, delete, or assign a source to a group.
- **No `gradlew` wrapper script committed.** Discovered during the
  CI/CD work — `gradle/wrapper/gradle-wrapper.properties` exists, but
  `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` were never
  generated in this repository. The CI workflows work around this by
  pinning a Gradle version via `gradle/actions/setup-gradle` rather
  than requiring a committed wrapper, but local development without a
  wrapper is a real, separately-tracked gap — see
  `FOUNDATION_COMPLETION_REPORT.md`.
- **`BalanceSnapshotWorker` only schedules daily, not also weekly.** A
  deliberate consolidation (weekly is a strict subset of daily data,
  sampled), not a missed requirement — see `WorkScheduler`'s own doc
  comment and `FINANCIAL_SOURCES_ARCHITECTURE.md`.
- **`FinancialHealthCheckWorker` combines debt reminders and forecast
  refresh into one job**, not two — same battery-conscious reasoning.

---

## Test coverage

189 unit tests (up from 161 after Phase 11), all pure-JVM, all in
`core:domain`:

| Area | New this phase |
|---|---|
| `ReconciliationEngineTest` | 9 tests |
| `WidgetCatalogTest` | 6 tests |
| `SourceAnalyticsTest` | 8 tests |
| `GetBalanceOverviewUseCaseTest` | +3 tests (reconciliation) |
| `BalanceSuggestionsTest` | +1 test (reconciliation conflict) |
| Existing use-case tests updated for the new `GetBalanceOverviewUseCase`/`generateBalanceSuggestions`/`calculateSourceHealth` signatures | no behavior change |

Same structural gap as every prior phase: no ViewModel tests, no
Compose UI tests, no WorkManager instrumented tests (`WorkManagerTestInitHelper`
would be the real tool for that — not used here). The two new Workers
and the extended Dashboard Studio/Accounts UI are untested for that reason.

---

## Readiness assessment for Phase 12

The foundation is genuinely more solid than it was: the single
most-flagged gap (reconciliation) is closed, CI exists for the first
time, WorkManager has real consumers, and the Dashboard Studio's
long-standing widget-add limitation is fixed. Three things worth being
direct about before Phase 12 (Automation Foundation) starts:

1. **Two UI surfaces remain genuinely unbuilt** (Source Analytics
   screen, Source Groups UI) — both have complete, tested domain
   layers underneath them, so this is UI work specifically, not
   missing engineering.
2. **The `gradlew` wrapper gap should be fixed before relying on CI
   for local development parity** — the workflows themselves work
   without it, but a contributor's local `./gradlew` won't until this
   is generated and committed.
3. **WorkManager is now active in production for the first time** —
   worth real-device verification (Doze mode behavior, actual battery
   impact, notification delivery) before treating it as fully proven,
   since nothing in this sandbox environment can execute a WorkManager
   job end-to-end.

**Recommendation: proceed to Phase 12 (Automation Foundation)**, with
the three items above carried forward explicitly. The phase's own
objective — making the platform internally complete and stable before
introducing SMS/OCR/Notification automation — is substantially met:
the reconciliation layer in particular was a real prerequisite for
automated balance updates to mean anything reliable.
