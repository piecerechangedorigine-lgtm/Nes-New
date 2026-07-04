# Nova — Dashboard Architecture

Everything below reflects what's actually in the repository as of Phase
9 (Dream Dashboard), not an aspirational target. See `ARCHITECTURE.md`
for the rest of the app; this doc is specifically about the
customizable dashboard system.

---

## The core idea

The Dashboard used to be one fixed screen (`DashboardContent` rendering
a hard-coded hero card, quick actions, insight banner, forecast banner,
and recent-activity list, in that order, always). Phase 9 replaces
everything below the hero card and quick actions with a **persisted,
person-configurable list of widgets** — the same financial data, but
which cards show, in what order, at what size, and how a goal renders,
are all now choices the person makes and Nova remembers.

The hero balance card and the four quick actions (Add / Accounts /
Budgets / Goals) stay fixed at the top, outside the widget system. This
was a deliberate scope call, not an oversight: they're navigational
chrome and the one "how much do I have" number every screen assumes is
always visible, not configurable content a preset would reasonably
hide. `DashboardWidgetType` has no "hero balance" or "quick actions"
entry — there was never an intent to make those configurable.

---

## Domain model

```
DashboardLayout
├── widgets: List<DashboardWidgetConfig>
├── activePreset: DashboardPreset?     — null once manually customized
└── background: DreamBackground

DashboardWidgetConfig
├── id: String                         — stable, deterministic (never random)
├── type: DashboardWidgetType          — GOAL / FORECAST / FINANCIAL_OVERVIEW / AI_INSIGHTS / RECENT_ACTIVITY / DEBT_OVERVIEW / DEBT_WEATHER / DEBT_COACH / DEBT_RECOVERY (Phase 10) / SOURCE_HEALTH / BALANCE_FORECAST (Phase 11 — see FINANCIAL_SOURCES_ARCHITECTURE.md)
├── isVisible: Boolean
├── order: Int
├── size: WidgetSize                   — SMALL / MEDIUM / LARGE
├── goalId: String?                    — only for GOAL widgets
└── visualizationMode: GoalVisualizationMode  — only meaningful for GOAL widgets
```

One `DashboardWidgetType.GOAL` widget config exists per goal that has
one — a person can have zero, one, or many, unlike every other type
which is a true singleton (`FORECAST`, `FINANCIAL_OVERVIEW`,
`AI_INSIGHTS`, `RECENT_ACTIVITY` each appear at most once). This is why
`DashboardWidgetConfig.id` for a goal widget is derived
(`"goal_<goalId>"`, see `DashboardLayoutDefaults.goalWidgetId`) rather
than random — the goal *is* the widget's identity.

### Why 9.1's "resize" is real but incomplete

`WidgetSize` persists, the Dashboard Studio UI to change it works, and
every widget composable's signature already takes a `size: WidgetSize`
parameter — but today every widget's actual layout renders identically
regardless of the value (`SMALL`/`MEDIUM`/`LARGE` differ only in minor
details like how many recent transactions show, or whether a secondary
stat line renders). This is exactly what "future-ready architecture"
in the 9.1 brief asked for: the data model and the UI control both
work end-to-end; building three genuinely distinct visual densities
per widget type is real UI work that didn't happen this phase. See
`TECHNICAL_DEBT.md`.

---

## Why presets are selection weights, not pixel percentages

The 9.2 brief describes presets as literal splits — "40% Dreams / 40%
Finance / 20% AI." Nova's dashboard is a vertically scrolling list, not
a fixed grid with regions to divide, and the number of `GOAL` widgets
varies with how many goals a person actually has (zero to a dozen). A
literal pixel-area split has no stable meaning under those conditions.

What a preset (`DashboardPreset`) concretely controls instead:

1. **Which widget types appear at all** — `widgetSizeFor(type)`
   returning `null` means that type is hidden in this preset.
2. **How large each visible type renders** — the `WidgetSize` a preset
   assigns (subject to the "incomplete" caveat above).
3. **How many goal widgets show** — `maxGoalWidgets`. `null` means one
   per goal, uncapped. A number means "show only the N most urgent
   goals," where urgency is the lowest `GoalHealth.score` — reusing
   Phase 8.5's scoring engine rather than inventing a second notion of
   "which goal matters most."
4. **Display order** — `typeOrder`.

This is the "feel" the brief's percentages describe, expressed as
rules that hold regardless of how many goals or transactions a real
person has, rather than a number that would silently stop meaning
anything the moment someone has more than a couple of goals.

### Switching presets is destructive, on purpose

`ApplyDashboardPresetUseCase` rebuilds the entire widget list from
scratch. Any manual customization — hidden widgets, custom order,
resized cards — is discarded. This is why applying a preset is its own
explicit action (`DashboardStudioViewModel.onApplyPreset`) distinct
from every other mutation, and why the Studio screen shows preset
chips as a separate section with its own explanatory line ("Switching
a preset rebuilds your widget list") rather than folding preset
selection into the ordinary widget-editing flow. The background image
is the one thing a preset switch preserves — it's a personalization
choice orthogonal to widget composition, not part of what a preset
describes.

---

## Goal Visualization Modes (9.4)

Five modes, one dispatch point (`GoalWidgetVisualization` in
`core:designsystem`'s `GoalWidgetRenderer.kt`), each a private
`@Composable` reading the exact same `GoalForecast` (and, where
relevant, the `GoalHealth` computed from it via Phase 8.5's
`GoalForecast.health()`):

| Mode | What it actually draws |
|---|---|
| Ring | A circular progress ring (`Canvas` arc), percentage in the center |
| Timeline | Linear progress bar with creation date and target date as endpoints |
| Horizon | A rising fill against a baseline — "how close to the top" as a bar-chart metaphor, not a literal skyline illustration (no artwork exists for that) |
| Velocity | Two comparative bars: actual average monthly saving vs. `requiredMonthlyContribution` — a linear comparison, not a true speedometer gauge shape |
| Weather | `GoalHealthLabel` expressed as a weather icon/word (sunny → stormy) — the one mode that's a direct read of Goal Health rather than a fresh computation over the forecast |

Adding a sixth mode is one new enum entry in `GoalVisualizationMode`
plus one new `when` branch in `GoalWidgetVisualization` — never a
data-layer change, since every mode already receives everything it
could need.

---

## The four financial widgets (9.5–9.7, plus Recent Activity)

All four read from **one** combined data source,
`GetDreamDashboardDataUseCase` — not four separate use case calls per
widget. It composes exactly the use cases that already existed before
Phase 9: `GetDashboardSummaryUseCase`, `GetForecastSummaryUseCase`
(8.5.6), `GetBalanceOverviewUseCase` (8.5.3 — this is also the phase
that finally gives it a real UI surface; Phase 8.5's own completion
report flagged it as built-but-invisible), `GetGoalForecastUseCase`,
and `GetAssistantContextUseCase` feeding `AssistantInsightEngine`
(9.7's "use current assistant implementation, do not integrate LLMs" —
satisfied by literally asking the existing engine the same question a
person might, and reading its reply's `actions` list as the "suggested
action" the brief asks for).

Phase 9 built the configurable widget system around this data. It did
not invent any new financial calculation — every number on every
widget was already computed, tested, and (except for Balance Overview)
already visible somewhere before this phase.

---

## Dream Background System (9.8)

`DreamBackground` is a `sealed class`, not an enum + nullable URI:

- `None` — default, no background.
- `DeviceImage(uri: String)` — a real photo picked via
  `ActivityResultContracts.OpenDocument()` in `DashboardStudioScreen`.
  The picker takes a **persistable** URI permission grant
  (`ContentResolver.takePersistableUriPermission`) at pick time — a
  URI without that grant stops resolving the next time the app process
  restarts, a common enough Storage-Access-Framework footgun that it's
  called out explicitly in the code, not just here.
- `AiGenerated` — modeled, not implemented. Selecting it anywhere
  would be premature since there's no generation pipeline behind it
  (see the 9.8 brief's own "Future AI-generated image support" and
  `ROADMAP_NEXT.md`). The case exists now purely so the persisted
  schema doesn't need to change shape once it is implemented.

**Readability enforcement**: `DreamBackgroundLayer` in
`DashboardScreen.kt` draws the picked image full-bleed, then a
vertical scrim gradient (background color at 55% alpha at the top,
92% at the bottom) over it unconditionally — every widget's text
renders against the scrim, never the raw image. This isn't
per-image-adjusted (no brightness/contrast analysis of the picked
photo); it's one gradient dark enough to guarantee legibility against
any image, which is what "never reduce readability" in the brief
actually requires — a guarantee, not a best-effort.

---

## Persistence

`DashboardRepository` persists the entire `DashboardLayout` as one
JSON blob under one DataStore key (`dashboard_layout_json`), using the
same `kotlinx.serialization` infrastructure Phase 8.5's Backup &
Restore introduced. This is a deliberate difference from
`ProfileRepositoryImpl`'s many independent keys: a widget list is one
cohesive unit that's always read and written together as a whole, so
there's no risk of two keys drifting out of sync the way independent
boolean toggles never could anyway.

A corrupted or unparseable persisted layout (`parseDashboardLayoutOrNull`
returning `null`) falls back to a fresh `BALANCED` layout built from
the person's actual current goals — silently, with no error shown.
This is a deliberately different failure mode from Backup & Restore's
`parseBackupPayload`, which throws a descriptive `InvalidBackupException`
for the UI to surface: losing a custom widget arrangement to a parse
error is a minor inconvenience, not a data-loss event, and doesn't
deserve the same "explain what went wrong" treatment financial data
does.

No Room migration was needed for any of Phase 9 — the entire
Dashboard Studio system lives in DataStore, not the SQLite database.

---

## 9.9 — Home Screen Widgets: what "Version 1" means here

Three Glance (Jetpack Compose-for-widgets) app widgets, all reading
live data through one Hilt `@EntryPoint` (`WidgetEntryPoint`) since
Glance widgets are constructed by the Android system via
`GlanceAppWidgetReceiver`, not by Hilt — there's no `@AndroidEntryPoint`
equivalent for `GlanceAppWidget`, and `EntryPointAccessors.fromApplication`
is the standard, documented way to reach the Hilt graph from a class
Hilt doesn't construct.

| Widget | Shows | Data source |
|---|---|---|
| Goal Progress | The single most urgent goal (lowest Goal Health score) — name, %, remaining | `GetDreamDashboardDataUseCase` |
| Forecast | This month's forecast message | same |
| Financial Snapshot | Available Spending Power, Total Liquidity | same |

**What "Version 1" honestly means**: each widget fetches a single
snapshot (`Flow.first()`, not a live subscription — see the code
comment on why `.collect` would hang the widget forever, since Room
and DataStore flows never complete on their own) and refreshes on
Android's standard `updatePeriodMillis` schedule, set to 30 minutes —
the minimum the platform allows. This is not a live/real-time surface;
opening the app and changing something won't update the widget until
the next scheduled refresh (or the person removes and re-adds it,
which forces one). That's the same constraint every Android home
screen widget lives under, not a shortcut specific to Nova.

The preview image every widget shows in the system's widget picker
(`res/drawable/widget_preview.xml`) is a plain solid rounded rectangle
in Nova's surface color — not a fabricated screenshot of content that
might not match what actually renders. It should be replaced with a
real captured screenshot once these widgets have run on a real device.

---

## 9.10 — Lock Screen Widgets: the honest technical answer

**Android has no public lock-screen-widget API.** Keyguard widgets
existed briefly in early Android (up to API 20) and were removed
starting Android 5.0 (API 21) — there has been no first-party way for
a third-party app to register a widget that renders specifically on
the lock screen for over a decade. Some OEM launchers/lock screens
(certain Samsung, Xiaomi, and other manufacturer skins) offer their
own proprietary lock-screen widget surfaces, but these consume
standard `AppWidgetProvider` registrations through vendor-specific
integration Nova has no visibility into and cannot target directly —
there is no separate manifest entry, no separate Glance API, no
separate anything to build for "lock screen" as its own target.

Given that, 9.10 is satisfied by 9.9: the same three
`AppWidgetProvider` registrations are everything there is to register.
Where an OEM's lock screen surfaces third-party widgets at all, it
does so by picking up these same providers — Nova doesn't need
(and couldn't build even if it wanted to) a separate lock-screen
code path. This is stated plainly rather than fabricating a call to an
API that doesn't exist, or shipping a manifest entry that would
silently do nothing on stock Android.

---

## 9.11 — Freedom First: how the architecture actually enforces it

The brief's core principle — "no mandatory layout... application
adapts to user preferences, users do not adapt to the application" —
isn't a UI-copy aspiration here; it's what the data model structurally
guarantees:

- **Nothing is hard-coded as always-visible** except the hero balance
  card and quick actions (a deliberate, narrow exception — see "The
  core idea" above), and that exception is total: every single
  `DashboardWidgetType` can be hidden, removed, resized, and reordered
  freely, including the ones a preset would normally show.
- **A preset is a starting point, never a constraint.** The instant
  someone customizes anything, `activePreset` becomes `null` — there's
  no "you can't do that, you're on the Minimal preset" validation
  anywhere in `DashboardStudioViewModel`. Every mutation method
  (`onToggleHidden`, `onRemove`, `onResize`, `onMoveUp`/`onMoveDown`,
  `onVisualizationModeChange`) operates on the widget list
  unconditionally.
- **An empty dashboard is a valid, supported state** — removing every
  widget doesn't fall back to a default; `DashboardContent` in
  `DashboardScreen.kt` renders an explicit empty state ("Your dashboard
  is empty... Open Dashboard Studio to add widgets") rather than
  silently re-adding something the person removed.
- **A widget referencing deleted data disappears, not crashes.** A
  `GOAL` widget whose goal was deleted since the widget was added
  renders nothing (`DashboardWidgetDispatcher` returns early on a
  missing lookup) rather than showing stale or default data pretending
  everything's fine.

Freedom First isn't a section of code — it's the absence of validation
that would otherwise say "no" to a person's choice about their own
dashboard.

---

## Postscript — Phase 11 added two widgets, deliberately not a third

`SOURCE_HEALTH` and `BALANCE_FORECAST` slot into the same widget
system this doc describes — same persistence, same Dashboard Studio
controls, same exhaustive-`when` safety net catching every preset that
needed a new branch. A third widget the 11.11 brief also asked for,
"Liquidity Overview" (Total Liquidity / Spending Power / Dream Safe
Balance), was **not** built — its spec is byte-for-byte what the
existing `FINANCIAL_OVERVIEW` widget (this doc, Phase 9) already
shows. Building a second, identical widget type would have been
exactly the duplicated logic this codebase's engineering rules rule
out. See `FINANCIAL_SOURCES_ARCHITECTURE.md` for the fuller reasoning.

The Dashboard Studio widget-add gap this doc's earlier postscript
flagged (only `GOAL` widgets can be dynamically re-added once a preset
hides a singleton type) is unchanged — it now also applies to
`SOURCE_HEALTH` and `BALANCE_FORECAST` on presets that hide them by
default (Balanced and Minimal, for both).

---

## Postscript 2 — Phase 11.5.4 closed the widget-add gap

Both postscripts above flagged the same limitation across two phases:
only `GOAL` widgets had any dynamic "add it back" path. Phase 11.5.4
closed it with `WidgetCatalog` — a real, searchable, categorized
(Goals/Debt/Forecast/Financial Sources/AI/System) browser in Dashboard
Studio. Adding a widget from the catalog and restoring a hidden one
are the same action now: if a config for that type already exists
(hidden by a preset or a prior removal), the catalog's "Add" button
just makes it visible again with its previous settings intact; if none
exists, it creates a fresh one. This is a deliberate unification, not
two features that happen to share a button — see
`FINANCIAL_SOURCES_ARCHITECTURE.md`'s widget catalog section and
`FOUNDATION_COMPLETION_REPORT.md` for the full reasoning.

---

## Postscript — a real gap Phase 10 surfaced

Phase 10 added four new widget types (`DEBT_OVERVIEW`, `DEBT_WEATHER`,
`DEBT_COACH`, `DEBT_RECOVERY` — see `DEBT_ARCHITECTURE.md`) and, in
wiring them up, exposed a limitation that existed since Phase 9 but had
no way to surface until there were more non-`GOAL` singleton widget
types to hit it: `DashboardStudioViewModel.onAddGoalWidget` is the only
dynamic "add a widget" path that exists. There's no generic "add back
a hidden singleton widget type" flow — a person on a preset that hides
`DEBT_WEATHER`/`DEBT_COACH`/`DEBT_RECOVERY` by default has no in-app
way to add just one of them back without switching presets entirely
(which resets everything else). Recorded here rather than only in
`DEBT_ARCHITECTURE.md` since it's really a Dashboard Studio gap, not a
debt-specific one — the next widget type added to any category will
hit the same wall.
