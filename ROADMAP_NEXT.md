# Nova — Roadmap: What's Next

This is the bridge document between the current codebase and
NESWALLET's longer-term vision. It exists so the next phase starts
from "here's exactly what foundation is real and what it enables"
rather than rediscovering it from the code.

---

## Foundations: now used, or still waiting

| Foundation | Status after Phase 11.5 |
|---|---|
| `FinancialSource`/`Debt` reconciliation | ✅ **Closed** — the most-referenced gap across Phases 10-11 is resolved (11.5.1) |
| Dashboard Studio's dynamic widget-add flow | ✅ **Closed** — `WidgetCatalog` replaces the `GOAL`-only limitation (11.5.4) |
| CI/CD | ✅ **New** — real GitHub Actions gate on every push/PR/release tag (11.5.7) |
| `WorkManager` | ✅ **Activated** — two real periodic workers after sitting unused since Phase 1 (11.5.3) |
| `BalanceSnapshotRepository` | ✅ **Has a real producer now** — `BalanceSnapshotWorker`, closing the "storage exists, nothing populates it" gap from Phase 11 |
| Debt Notifications (10.12) | ✅ **Implemented** — via `FinancialHealthCheckWorker`, not a dedicated feature, but functionally real |
| Source Analytics UI | 🔴 Still unbuilt — domain layer complete (11.5.2) |
| Source Groups UI | 🔴 Still unbuilt — domain layer has existed since Phase 11, untouched this phase |
| `gradlew` wrapper | 🔴 **New gap found** — never committed to this repo; CI works around it, local dev doesn't yet |
| `AssistantInsightEngine` | 🟡 Unchanged — Smart Balance Suggestions is still a separate, similarly-structured engine |
| `ProfileSettings.currency` | 🟡 Still persisted, still not threaded through `Money.formatted()` — unchanged since Phase 8.5 |
| `NovaSoundManager` | 🟡 Still not called from any screen — unchanged since Phase 8.5 |
| `BalanceUpdateMode.ASSISTED`/`.SMART` | 🟡 Still inert — but now sit behind a genuinely reconciled, tested balance layer, which is exactly what Phase 12's automation work needs underneath it |

---

## Immediate next steps

1. **Generate and commit the `gradlew` wrapper.** A five-minute fix
   (`gradle wrapper --gradle-version 8.10`) that closes the one gap
   this phase's own CI work surfaced rather than fully resolved.
2. **Build the Source Analytics screen.** Three of five chart types
   need no new engineering — `GetSourceAnalyticsUseCase` is ready.
3. **Build Source Groups UI.** Domain and persistence have been ready
   since Phase 11; this is purely a UI gap now.
4. **Verify WorkManager on a real device.** Nothing in this
   development environment can execute a periodic job end-to-end —
   Doze-mode behavior, actual notification delivery, and real battery
   cost all need real-device confirmation before this is fully trusted.
5. **Consider a settings toggle for reconciliation ownership** — today
   the `Debt` always wins over a linked `FinancialSource`'s own
   balance; a person who disagrees with that has no way to flip it.

---

## Phase 12: Automation Foundation, and what it inherits

This phase's own objective was explicit: close gaps *before*
SMS/OCR/Notification automation begins, specifically because
automated balance updates only mean something reliable once the
balance layer underneath them is internally consistent. Concretely,
what Phase 12 now has that it wouldn't have without this phase:

- **A reconciled liability layer.** An SMS-parsed or OCR-scanned
  credit card balance update needs to know whether that card is linked
  to a `Debt` and, if so, which record an automated update should
  actually write to — `effectiveLiabilityBalance`'s ownership rule is
  the answer this phase already worked out for manual edits, and
  automation should follow the same rule rather than inventing a
  second one.
- **A real notification pipeline.** `FinancialHealthCheckWorker` and
  `NotificationHelper` are the concrete precedent for how any future
  automation-driven notification (e.g. "a suggested balance update is
  ready to review") should be built — same channel, same permission
  check, same Freedom-First "suggest, never auto-apply" pattern.
- **A real WorkManager scheduling pattern.** `WorkScheduler`'s
  battery-conscious constraints and unique-work-naming approach is now
  the template for however SMS/OCR automation's own background work
  (parsing, syncing) gets scheduled.
- **Real CI** to catch a regression in any of the above before it ships.

None of this is scoped or estimated for Phase 12 itself — this section
is a map of what's now available to build on, not a commitment.

---

## Longer-term, still unscoped

- **Financial Twin** — still no domain model, use case, or UI concept,
  across four phases of being mentioned as a scope exclusion.
  `BalanceSnapshot` history (now actually accumulating, thanks to
  11.5.3) is the first real building block it would need.
- **SMS-based balance suggestions / OCR statement scanning** — this is
  what Phase 12 is expected to scope; see above for what it inherits.
- **Real AI Assistant (LLM-backed)** — still needs a server-side proxy
  Nova doesn't have.
- **Multi-currency** (`Money.formatted()` still USD-only regardless of
  `ProfileSettings.currency`) — unchanged, low priority relative to
  everything above.

---

## What Phase 11.5 deliberately did not do

- No Source Analytics UI or Source Groups UI — both real, honest scope
  cuts given this phase's own breadth; see `PROJECT_STATUS.md`.
- No `gradlew` wrapper generation — discovered, not fixed, this phase.
- No real-device verification of the new WorkManager jobs.
- No settings toggle for reconciliation ownership direction — `Debt`
  always wins today, by design, with no override.
- No automatic weekly balance snapshot job separate from the daily
  one — a deliberate consolidation, not a missed requirement.
