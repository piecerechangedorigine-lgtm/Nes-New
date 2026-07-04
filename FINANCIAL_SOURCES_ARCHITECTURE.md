# Nova — Financial Sources Architecture

Everything below reflects what's actually in the repository as of Phase
11 (Smart Financial Sources & Balance Intelligence). See
`ARCHITECTURE.md` for the rest of the app, `DEBT_ARCHITECTURE.md` for
how Debt Health feeds into Balance Health here, and
`DASHBOARD_ARCHITECTURE.md` for how the new widgets fit the Dream
Dashboard.

---

## Why the Balance Intelligence Engine kept its Phase 8.5 names

The 11.3 brief asks to "Create: BalanceIntelligenceEngine." What
actually happened: `BalanceOverview` (the model) and
`GetBalanceOverviewUseCase` (the use case) — both from Phase 8.5 —
were extended with two new fields (`emergencyReserve`,
`forecastBalance`) and rewritten to respect the new per-source
inclusion flags (11.4). Neither was renamed.

This is the same pragmatic call Phase 8.5 itself made keeping
`Transaction.accountId`'s name after the `Account` → `FinancialSource`
rename: a purely cosmetic rename would have touched every existing
consumer — the Dashboard, the Financial Overview widget, the home
screen widget, `GetDebtSummaryUseCase`, `GetFinancialSourceIntelligenceUseCase`
— for zero behavioral benefit. The "Balance Intelligence Engine" the
brief describes is real; it just lives inside the type names that
already existed rather than new ones.

---

## Per-source inclusion controls (11.4) — how they actually flow through

Every `FinancialSource` carries five boolean flags plus one more for
emergency reserve status, **all defaulting to `true`** (except
`isEmergencyReserve`, which defaults `false` — see the field's own doc
comment for why). `GetBalanceOverviewUseCase` reads them like this:

| Calculation | Filtered by |
|---|---|
| `totalLiquidity` | `includeInLiquidity` |
| `availableSpendingPower` | `includeInSpendingPower` (a *different* filter from liquidity — a savings account can be liquid but excluded from day-to-day spending power, per 11.4's own example) |
| `forecastBalance` | `includeInForecast` |
| `emergencyReserve` | `isEmergencyReserve` (not one of the five inclusion flags — a separate, explicit declaration) |

**`includeInGoals` and `includeInAnalytics` are persisted, editable in
the Accounts form, and read by nothing yet.** Nova's goal calculations
(`dreamSafeBalance` = sum of `SavingsGoal.currentAmount`) have no
concept of "which source funded this goal" — goals aren't linked to
sources anywhere in this architecture — so there's no per-source
filter for `includeInGoals` to gate. Analytics similarly has no
per-source breakdown yet (see "Source Analytics" below). Both flags
exist now specifically so a future goal-to-source link or a
source-level analytics breakdown doesn't need a schema change to add
them — the same "field exists before the feature that reads it, so
the feature is additive when it ships" pattern `BalanceUpdateMode`
already established in Phase 8.5 for ASSISTED/SMART sources.

---

## Why `Debt` and `FinancialSource` still don't reconcile

Phase 10 flagged this as a known gap; Phase 11 didn't close it. A
credit card can be a `FinancialSource` (its spendable balance, feeding
liquidity) *and* a `Debt` (its payoff plan) with no link between the
two records. Phase 11's Credit Card Intelligence (11.5) computes
utilization from `FinancialSource.creditLimit`/`currentBalance` —
entirely independent of whatever a `Debt` record with the same
real-world card might say. Two independently-editable numbers
describing the same account is still possible. This needs a product
decision (see `ROADMAP_NEXT.md`), not more engineering guesswork.

---

## Credit Card Intelligence (11.5)

`FinancialSource.creditCardUtilization` is a computed property, not a
stored one — `null` unless the source is a `CREDIT_CARD` type *and*
has a `creditLimit` set (both conditions, not either). The threshold
bands (Healthy <30%, Moderate 30-50%, High 50-80%, Critical 80%+) are
standard real-world credit-utilization guidance, not a Nova invention.

"Integrate with Debt Intelligence. No automatic debt creation" (11.5's
own words) is satisfied narrowly: `generateBalanceSuggestions` (11.10)
reads a credit card's utilization and can suggest paying it down, the
same way it reads Debt Health for other suggestions — but nothing
anywhere in this codebase creates a `Debt` record from a
`FinancialSource`, automatically or otherwise. `DebtRepository.addDebt`
is only ever called from `DebtViewModel.submitForm`, which only ever
runs from an explicit person-initiated form submission.

---

## Source Health (11.9) vs. Balance Health (11.6) — genuinely different questions

Two 0-100 scores exist in this phase; they answer different questions
on purpose:

- **`calculateSourceHealth`** — "is *this* source's balance in good
  shape." A credit card is scored purely on utilization; everything
  else is scored on how many months of the person's own average
  monthly expense its balance could cover (a "buffer" proxy, target 6
  months), reusing the same technique `DebtPressureScore`'s liquidity
  factor already established.
- **`calculateBalanceHealth`** — "is the person's *whole* financial
  position sound." Four factors, and three of the four are direct
  reads of other engines' output rather than new calculations:
  liquidity and emergency coverage are buffer-style checks like
  Source Health's, but debt burden is a direct scaling of
  `DebtHealthScore.score` (Phase 10) and forecast stability reads
  `ForecastStatus`/`ForecastConfidence` (Phase 8.5) directly. Balance
  Health is a *synthesis* score — the one Phase 11 engine that reaches
  across every other intelligence engine in the app rather than
  computing something new from raw sources.

Both reuse `DebtHealthLabel` (HEALTHY/MODERATE/HIGH_RISK/CRITICAL) for
consistency with Debt Health's own labeling — except `BalanceHealthScore`,
which uses its own `BalanceHealthLabel` (HEALTHY/STABLE/WARNING/CRITICAL)
matching the exact four words the 11.6 brief specifies. Two different
four-tier enums exist because the brief asked for two different sets
of words for two conceptually different scores, not because of an
oversight.

---

## Source Forecasting (11.7) — same math, narrower input

`calculateSourceForecast` is deliberately *not* a second forecasting
algorithm. It's the exact same linear spend-pace projection
`GetForecastSummaryUseCase` (8.5.6) already runs for the whole
account — extrapolate this month's per-day net rate across the
remaining days — scoped down to one source's own transactions instead
of every transaction. `TransactionRepository` already supported
per-source filtering (`observeTransactions(accountId)`), so this
needed no new repository capability, only the projection math applied
to a narrower input.

`GetFinancialSourceIntelligenceUseCase` computes every source's
forecast from **one** `observeTransactionsForMonth` call, grouped by
`accountId` in memory — not one repository query per source. The same
"single combined read, not N queries" principle every other aggregate
use case in this app already follows.

---

## Balance Snapshots (11.8) — infrastructure, honestly scoped

The 11.8 brief frames this explicitly as forward-looking
infrastructure ("Used later for: Trends, Financial Twin, AI
Insights"). What Phase 11 actually built: the `balance_snapshots`
table, `BalanceSnapshotRepository`, and the ability to record a
snapshot on demand. **What it did not build: automatic daily/weekly/
monthly capture.** That needs a real scheduler — `WorkManager` has sat
in `core:data`'s dependencies unused since Phase 1, flagged repeatedly
in `TECHNICAL_DEBT.md` and `ROADMAP_NEXT.md`, and this phase didn't
change that. There is currently no code path anywhere in the app that
calls `BalanceSnapshotRepository.recordSnapshot` at all — the storage
exists; nothing populates it yet. Stated plainly here rather than
looking finished from the outside.

---

## Smart Balance Suggestions (11.10) — the same structural guarantee as the Debt Simulator

"Suggestions never execute automatically" is enforced the identical
way Phase 10's `simulateDebtScenario` enforces "no data modified until
confirmed": `generateBalanceSuggestions`'s signature has no repository
to write through. It returns `List<BalanceSuggestion>` — plain text
and a priority — never an action a caller could accidentally execute.

Every suggestion is a direct, checkable read of numbers this app
already computed elsewhere — critical/high credit utilization, an
emergency reserve covering less than a month *with real spare Spending
Power to actually draw from* (a suggestion to move money that isn't
actually free would be exactly the kind of unchecked recommendation
this engine is built to avoid), and a goal underfunded within two
months of its deadline. Nothing fabricated, nothing with no number
behind it.

---

## Source Groups (11.2) — deliberately inert data

`SourceGroup` is DataStore-backed JSON (same pattern
`DashboardRepositoryImpl` already established for the widget layout in
Phase 9), not a Room table — groups have no referential integrity
worth enforcing (a group being deleted while sources still reference
its id doesn't corrupt anything; `FinancialSource.groupId` just
becomes a dangling label with no group to show for it, which the
Accounts screen would need to handle gracefully whenever it actually
reads `groupId` — see "What Phase 11 didn't build" below).

**Nothing in `BalanceOverview`, `BalanceHealthScore`, or any other
Phase 11 calculation reads `FinancialSource.groupId` at all.** Groups
are purely for the person's own mental model of their sources — "Daily
Spending" vs. "Savings" vs. "Travel" — never an input to any scoring
engine. This is a deliberate scope boundary: 11.2's own brief says
"Groups are optional," and making a score depend on optional metadata
would make that score inconsistent depending on whether someone
bothered to organize their sources.

---

## What Phase 11 didn't build

Stated plainly rather than left to be discovered:

- **No Source Groups UI.** The domain model, repository, and five
  suggested defaults (`SuggestedSourceGroups`) all exist and are real;
  there is no screen or form control anywhere that lets a person
  create a group, assign a source to one, or see sources organized by
  group. `FinancialSource.groupId` can be set programmatically but has
  no UI path to be set by a person.
- **No automatic Balance Snapshot capture.** Covered above — storage
  exists, nothing populates it.
- **No dedicated Source Analytics screens** (11.12's five types —
  Balance Distribution, Source Allocation, Liquidity Trends, Savings
  Growth, Credit Utilization). The *data* for several of these already
  exists in engines this phase built (credit utilization from 11.5,
  per-source balances from the source list itself), but no chart or
  dedicated Analytics tab renders any of it. This is the single
  largest unbuilt UI surface from this phase's brief.
- **No FinancialSource/Debt reconciliation** — covered above.
- **Advanced onboarding fields are all-or-nothing, not per-field
  optional the way the brief describes.** 11.13 lists "Initial
  Balance, Credit Limit, Goal Assignment, Source Group" as independent
  optional setup steps. What actually shipped: Initial Balance (always
  present, it's the core balance field), Credit Limit (progressive
  disclosure — appears only for Credit Card type), and a collapsed
  "Advanced options" section covering the 11.4 inclusion controls.
  Goal Assignment during onboarding was not built at all (there's no
  source-to-goal link in the data model for it to assign — see the
  `includeInGoals` discussion above), and Source Group assignment
  during onboarding wasn't built either, consistent with "no Source
  Groups UI" above.

---

# Phase 11.5 — Foundation Completion

Everything below was added in the dedicated hardening/consolidation
pass that followed Phase 11, closing gaps this document itself had
flagged.

## The reconciliation layer (11.5.1)

`FinancialSource.linkedDebtId` is the one new field: a nullable
reference to a `Debt.id`. Everything else is a function reading it.

**The ownership rule**: when linked, the `Debt` is authoritative for
the balance owed — not the `FinancialSource`. `effectiveLiabilityBalance(source,
debts)` is the single function every liability-aware calculation now
goes through instead of reading `FinancialSource.currentBalance`
directly:

- `GetBalanceOverviewUseCase.netBalance` — liquidity/spending-power/
  forecast math all use it.
- `effectiveCreditCardUtilization(source, debts)` — the reconciled
  form of the property-based `FinancialSource.creditCardUtilization`,
  used by `calculateSourceHealth`, `generateBalanceSuggestions`, the
  Accounts screen's utilization badge, and `SourceAnalytics`'s credit
  utilization chart. The raw, unreconciled property still exists (it's
  what `effectiveCreditCardUtilization` falls back to for an unlinked
  card) but nothing consumer-facing reads it directly anymore.

**Why the `Debt` wins, not the `FinancialSource`**: a `Debt` carries
richer payoff-plan metadata (interest rate, minimum payment, due date)
that a person is more likely to be actively maintaining once they've
bothered to link the two records at all. This is a product judgment
call, not a mathematical necessity — a future settings toggle letting
the person choose which side wins is a reasonable extension, not built
here.

**Conflicts are surfaced, not just resolved.** `detectReconciliationConflicts`
compares the two balances directly (with a small tolerance for
rounding) and, when they've drifted, produces a `BalanceSuggestion`
naming both figures. Reconciliation makes every *calculation* agree
regardless of drift; the conflict suggestion is what actually prompts
a person to go fix the drift itself.

**What this doesn't do**: nothing here automatically edits either
record to match the other. A person still has to update one manually
after seeing the conflict suggestion — matching this whole phase's own
"no forced configuration" principle.

## Widget Catalog (11.5.4)

`WidgetCatalog.entries` is a static list — one entry per non-`GOAL`
`DashboardWidgetType`, each with a display name, description, and one
of the six categories the brief specifies. `WidgetCatalog.search(query)`
matches against both the display name and the category name, so
searching "forecast" surfaces both the Monthly Forecast and Balance
Forecast widgets even though only one has "forecast" in its own name.

The Dashboard Studio's `onAddWidget(entry)` is genuinely one function
serving two purposes the brief lists separately ("Add Widget Flow" and
"Restore Hidden Widgets"): it looks for an existing config of that
type first — if found, it flips `isVisible` back to `true` (a restore,
preserving whatever size/order it already had); if not found, it
creates a fresh `WidgetSize.MEDIUM` config appended to the end (an
add). The Studio screen's catalog list itself already excludes
whichever types are currently visible, so a person only ever sees "add
this" as an option for something that isn't already showing — hidden
widgets and never-added widgets sit in the same list without needing a
separate "restore" section to browse.

## Source Analytics (11.5.2) — domain-complete, UI-pending

`calculateSourceAnalytics` and `GetSourceAnalyticsUseCase` are real and
tested. Three of five outputs (`balanceDistribution`, `sourceAllocation`,
`creditUtilizations`) need only current data — no history required.
The other two (`liquidityTrend`, `savingsGrowthTrend`) are day-bucketed
sums over `BalanceSnapshotRepository.observeAllSnapshots()`, computed
in the use case (not the pure function, which stays a simple
aggregator) since bucketing by calendar day is a genuine "how should
this be grouped for display" decision rather than a pure calculation.
`savingsGrowthTrend` scopes to `SAVINGS_ACCOUNT`-type sources
specifically — a reasonable, stated interpretation of "savings
growth" distinct from the portfolio-wide `liquidityTrend`, not the
only possible one.

No screen renders any of this yet. See `PROJECT_STATUS.md`'s "What
wasn't built" for why this was the deliberate scope cut this phase
made, and the explicit precedent (`GetBalanceOverviewUseCase` in Phase
8.5) for why "engine complete, UI later" is a normal, tracked state in
this codebase rather than an oversight.

## WorkManager Activation (11.5.3)

Two workers, both `@HiltWorker`-annotated, both scheduled once from
`NovaApplication.onCreate()` via `WorkScheduler`:

- **`BalanceSnapshotWorker`** — records one `BalanceSnapshot` per
  active source every run. Scheduled **daily only**, not also weekly
  as the brief separately lists — a weekly trend is trivially
  derivable from daily data by sampling every seventh point, so a
  second, always-running weekly job would only double the storage and
  battery cost for zero new analytical capability. Documented here as
  a deliberate consolidation.
- **`FinancialHealthCheckWorker`** — checks for overdue debts and a
  forecasted deficit, posting a notification for either. This
  consolidates the brief's separate "Debt Reminder Scheduling" and
  "Forecast Refresh Jobs" into one job, for the same battery-conscious
  reasoning. It's also the first real, working implementation of Debt
  Notifications (10.12), which Phase 10 explicitly deferred.

**Battery consciousness, concretely**: both jobs run every 24 hours
(WorkManager's Doze-friendly floor for anything not truly urgent),
both require `Constraints.setRequiresBatteryNotLow(true)`, neither
requires network (everything either reads is already local), and both
use `ExistingPeriodicWorkPolicy.KEEP` so re-calling `scheduleAll()` on
every app launch never resets an already-scheduled job's countdown.

**Retry policy**: `BackoffPolicy.EXPONENTIAL` starting at 15 minutes on
both — a transient failure (a locked database mid-write, for example)
gets retried with growing delay rather than silently giving up until
the next scheduled run a full day later.

**Notifications respect `ProfileSettings.areNotificationsEnabled`**:
`FinancialHealthCheckWorker` still runs and still computes on its full
schedule regardless of that setting (so the moment someone re-enables
notifications, the very next run reflects current data), it just
doesn't call `postFinancialHealthNotification` while the setting is
off. The real Android runtime `POST_NOTIFICATIONS` grant is checked
separately, on every post attempt — a person can revoke that in system
settings independent of what Nova's own toggle says, and posting
without checking would throw on API 33+.

**What wasn't verified**: nothing in this sandbox can actually execute
a `WorkManager` job end-to-end (no emulator, no real Doze-mode
scheduler). The code is real, correctly structured, and follows
documented AndroidX patterns throughout, but real-device verification
(does the notification actually appear, does Doze mode delay it as
expected, what's the actual measured battery cost) hasn't happened —
see `FOUNDATION_COMPLETION_REPORT.md`.
