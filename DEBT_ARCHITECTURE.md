# Nova — Debt Architecture

Everything below reflects what's actually in the repository as of Phase
10 (Debt Intelligence Center). See `ARCHITECTURE.md` for the rest of
the app and `DASHBOARD_ARCHITECTURE.md` for how Debt widgets fit into
the Dream Dashboard specifically.

---

## Why `Debt` is a separate entity from `FinancialSource`

Before this phase, the only way to represent debt at all was a
`CREDIT_CARD`-type `FinancialSource` — a bare balance with no
direction, no due date, no interest rate, no payoff strategy. Rather
than bolt all of that onto `FinancialSource` (which needs to stay a
simple "where does money sit" abstraction for liquidity purposes),
Phase 10 introduces `Debt` as its own first-class entity.

**This means a credit card can reasonably be represented twice** — once
as a `FinancialSource` (its spendable balance, feeding
`GetBalanceOverviewUseCase`'s liquidity math) and once as a `Debt`
(its payoff plan, feeding Debt Health/Weather/Freedom Date). **Nothing
reconciles the two automatically if someone does that.** Entering a
$500 balance on both is easy to do and nothing here catches it. This
is a deliberate scope boundary, not an oversight — unifying the two
models would be a much larger architectural change than this phase's
brief asked for.

---

## The five engines

All five are pure functions or thin composed use cases over
`List<Debt>` plus data Nova already computes elsewhere — none of them
introduce a new source of truth for income or liquidity.

### Debt Health Score (10.3)

`calculateDebtHealth(debts, monthlyIncome, totalLiquidity)` — five
weighted factors (debt size vs. annual income, monthly obligations vs.
income, number of open debts, average repayment progress, total owed
vs. liquidity), 0–100, same "deliberately simple heuristics, not a
model" approach as `GoalHealth` and the Dashboard's own insight banner.

Only `DebtDirection.I_OWE` debts count against the score — money owed
*to* the person is a receivable, not a burden, and doesn't improve the
score either. It shows up in Debt Pressure and the Debt Recovery
widget instead.

### Debt Weather (10.4)

A direct five-bucket mapping from the Health score (`weatherFor`), plus
a separate `trendFor` signal. The trend is **not** a real
"better/worse than last month" comparison — Nova doesn't persist
historical `DebtHealthScore` snapshots, so there's no history to
compare against yet. `trendFor` instead reads what's checkable right
now: any overdue `Debt.dueDate` on an active debt reads as
`WORSENING`; real repayment progress with nothing overdue reads as
`IMPROVING`; everything else is `STABLE`. This is stated plainly in
the code and here, not left for someone to discover the hard way.

### Debt Freedom Date / Smart Payoff Plans (10.5/10.6)

`simulatePayoffPlan` is a **real month-by-month amortization** — interest
accrues on each debt's balance before that month's payment is applied,
exactly how a real lender calculates it, not a `balance / payment`
division shortcut. This is what makes `LOWEST_INTEREST` (pay
highest-rate debts first — classic "avalanche") actually produce a
different, lower `totalInterestPaid` than `FASTEST_FREEDOM` (pay
smallest balances first — classic "snowball") when a scenario has
extra budget to allocate; without real interest accrual the two
strategies would only differ in *order* shown, never in outcome.

`BALANCED` ranks debts by combined balance-rank + interest-rank (both
ascending), a simple, explainable hybrid rather than a weighted formula
that would be harder to justify to someone asking "why this order."
`CUSTOM` takes an explicit person-supplied order and appends anything
they didn't rank at the end.

The simulation caps at 600 months (50 years) — a debt whose payment
can't outpace its own interest accrual would otherwise loop forever;
capping the horizon and returning `null` for `debtFreeDate` is the
honest answer for "this debt, as entered, can't be paid off."

`calculateDebtFreedomDate` (10.5) is just this same engine called with
`FASTEST_FREEDOM` and zero extra budget — the "if nothing changes"
baseline every `PayoffStrategy` projection is a variation on.

### Debt Pressure Score (10.9)

Deliberately distinct from Debt Health. Health asks "is your debt
position structurally sound" (size, progress, debt count). Pressure
asks a narrower question: "if something went wrong this month, how
much room do you actually have" — built from just two factors
(monthly obligations vs. income, and how many months of liquidity
would cover those obligations if income stopped). A person can have
excellent long-term Debt Health (small, well-progressed debt) and
still show real short-term Pressure if liquidity is thin — Pressure is
what catches that case Health alone wouldn't. Higher Pressure score
means *more* pressure, the inverse convention from Health's "higher is
better," and that inversion is intentional: Pressure is a burden
metric, Health is a wellness metric.

---

## Debt Simulator (10.7) — what "no data modified until confirmed" means structurally

`simulateDebtScenario(currentDebts, adjustments, goals, ...)` never
accepts a `DebtRepository`. This isn't a convention the ViewModel
happens to follow — the function's signature makes it structurally
impossible for a simulation to persist anything, since there's no
repository reference anywhere in its call graph to persist *through*.
A ViewModel can discard a `DebtSimulationResult` and nothing anywhere
else in the app would ever know the simulation happened.

Four adjustment types exist in the domain layer
(`DebtScenarioAdjustment`: increase payment, delay payment, add debt,
remove debt) — **the UI currently exposes two** (increase payment,
delay payment) in `DebtSimulatorScreen`. This is a deliberate, honest
scope cut for this phase, not a missing feature in the engine: the
other two are fully implemented and tested (`DebtSimulatorTest`), just
not wired into a form yet. Extending the screen to add/remove a
hypothetical debt is a UI-only change.

"Delay payment" required extending `simulatePayoffPlan` itself with an
optional `paymentDelayMonths` map (debt id → leading months with no
payment, interest still accruing) — empty by default, so it costs
nothing to every other caller (10.5's Debt Freedom Date, 10.6's payoff
strategies) that never uses it.

---

## Debt vs. Dream Impact (10.8)

`calculateDebtToGoalImpact(goal, monthlyCashFlowDelta)` answers "if
this much monthly cash flow frees up (or disappears), how does that
change when this goal finishes" — but Nova has no per-contribution
timestamp history (`GoalRepository.contribute()` only ever increments
a running total), so there's no real historical contribution *rate* to
project from. The baseline rate used here is the same proxy
`GoalHealth`'s pace component already relies on: total saved so far
divided by months since the goal was created. **This is a genuine
computed projection from real data — not a fabricated number — built
on an averaged proxy rather than true contribution history**, and that
distinction is stated here rather than left implicit. A goal with zero
historical saving rate and no cash flow change reports "not enough
contribution history" rather than a confident-looking wrong number.

The Debt Simulator computes the cash-flow delta it feeds into this as
the steady-state difference in total minimum monthly payments between
baseline and scenario — not a month-by-month reconstruction of exactly
when each dollar frees up. Reasonable for "roughly how much sooner,"
not precise to the month.

---

## AI Debt Coach (10.10) — literally the existing Assistant, extended

Per the brief's own instruction ("Use existing Assistant architecture.
No LLM integration."), this isn't a new engine — `AssistantContext`
gained a `debtSummary` field, and `AssistantInsightEngine` gained a
`debtReply()` branch (routed from queries containing "debt", "owe",
"loan," etc.) plus a dedicated `topDebtRecommendation()` function for
widget-sized output. Both read the exact same `DebtSummary` the Debt
Intelligence Center screen and every Debt widget already read — there
is no second, parallel debt-coaching data path.

`topDebtRecommendation()`'s priority order: an overdue debt beats
everything (a missed date is the most concrete, checkable fact
available); high/extreme Debt Pressure beats a routine suggestion;
otherwise it names the smallest active debt as the fastest one to
eliminate. This is the same "explainable, checkable logic" standard
every other Assistant reply already holds itself to — never a vague
"you should pay down debt" with nothing behind it.

---

## Debt Widgets (10.11) and Dashboard Integration (10.13)

Four new `DashboardWidgetType` entries (`DEBT_OVERVIEW`,
`DEBT_WEATHER`, `DEBT_COACH`, `DEBT_RECOVERY`) slot into the exact same
widget system Phase 9 built — same persistence, same Dashboard Studio
visibility/order/size controls, same `WidgetCategory` weighting
concept (all four are `FINANCE` category except `DEBT_COACH`, which is
`AI` since it's Assistant-generated). Every preset's `typeOrder` and
`widgetSizeFor` got exhaustive new branches for all four — Kotlin's
compiler enforced this: adding entries to a closed `enum` immediately
flags every non-exhaustive `when` across the codebase, which is
exactly the safety net that caught (at compile time, not runtime) every
place a size/order decision needed making for the new types.

**Known gap, found while wiring this up**: Dashboard Studio's
`onAddGoalWidget` is currently the *only* dynamic "add a widget" path —
there's no generic "add back a hidden singleton widget type" flow. A
person on the Dream Focus or Minimal preset (where `DEBT_WEATHER`,
`DEBT_COACH`, and `DEBT_RECOVERY` default to hidden — see
`DashboardPreset.widgetSizeFor`) has no in-app way to add those three
back individually; only switching to Finance Focus (which resets
everything else) surfaces them. `DEBT_OVERVIEW` itself is visible by
default in every preset, so this only affects the three more
specialized debt widgets. This is a real, currently-unaddressed gap —
not something Phase 10 fixed, and worth flagging clearly rather than
letting it be quietly rediscovered later.

`GetDreamDashboardDataUseCase` grew a sixth combined source
(`GetDebtSummaryUseCase`) plus the `topDebtRecommendation` computation
via `AssistantInsightEngine`. Six sources exceeds `combine`'s typed
overloads (five); the fix is the same nested-`combine` pattern the
five-source version already used, extended by one more group — not the
untyped `vararg Flow<*>` overload, which would have required unchecked
casts to pull typed values back out.

### Backup & Restore also gained debts

`BackupPayload.debts` (new field, defaults to an empty list) means a
pre-Phase-10 backup still parses correctly — `kotlinx.serialization`'s
default-value handling fills in `emptyList()` for a JSON payload that
never had a `debts` key at all. `CURRENT_BACKUP_VERSION` bumped to `2`
anyway, even though it wasn't strictly required for backward-reading
compatibility, since the schema's shape genuinely changed and a person
restoring an old v1 file should be able to tell that from the version
number alone.

---

## Freedom First (per this phase's stated philosophy)

"AI suggests. User decides. Everything remains optional and
configurable" — concretely, in this codebase:

- **Every AI-attributed number is a suggestion, never an automatic
  action.** `topDebtRecommendation()` returns a message plus a
  "Open debt center" action — it never reorders payoff priority, never
  reallocates a payment, never touches `DebtRepository` on its own.
- **Payoff strategy is always the person's explicit choice.**
  `simulatePayoffPlan` computes every strategy's outcome; nothing
  picks one automatically or applies it without the person choosing
  a `PayoffStrategy` value themselves.
- **The Simulator can never write.** Covered above — this is enforced
  by the function signature, not a UI convention that could be
  bypassed by a future screen forgetting to check a flag.
- **Every due date is optional**, both in the data model (10.2) and in
  practice — `calculateDebtHealth`, `calculateDebtWeather`, and
  `calculateDebtFreedomDate` all degrade gracefully (neutral scores,
  "not projectable yet" states) rather than demanding complete data
  before showing anything useful.
- **Every Debt widget can be hidden or removed** through the same
  Dashboard Studio controls every other widget uses — Debt gets no
  special "can't be turned off" treatment anywhere in this codebase.

---

## Postscript 2 — Phase 11.5.1 closed the reconciliation gap

The postscript above (Phase 11) described Credit Card Intelligence
computing utilization with no awareness of a linked `Debt`. Phase
11.5.1 closed that: `FinancialSource.linkedDebtId` plus
`effectiveLiabilityBalance`/`effectiveCreditCardUtilization` now make
the linked `Debt`'s balance authoritative everywhere a liability
figure is read — `GetBalanceOverviewUseCase`'s liquidity math, Source
Health, credit utilization display, and Smart Balance Suggestions all
agree now instead of computing independent answers. A
`ReconciliationConflict` detector also surfaces real drift between the
two records as a suggestion rather than resolving it silently. See
`FINANCIAL_SOURCES_ARCHITECTURE.md` for the full mechanics and
`FOUNDATION_COMPLETION_REPORT.md` for how this was verified.

---

## Postscript — Phase 11's Credit Card Intelligence didn't close the reconciliation gap

Phase 11 added `FinancialSource.creditCardUtilization` (limit, used,
available, utilization %) computed entirely from `FinancialSource`
fields — with no awareness of any `Debt` record that might describe
the same real-world credit card. A person can still enter a card as
both a `FinancialSource` (for its spendable/credit-line balance) and a
`Debt` (for its payoff plan), and now *both* representations compute
their own independent "how bad is this card" signal (Credit Card
Intelligence's utilization label vs. Debt Health's score) with no
cross-check between them. See `FINANCIAL_SOURCES_ARCHITECTURE.md` for
the fuller writeup — this is the same open gap Phase 10 flagged,
touched by a second phase's features without being resolved.
