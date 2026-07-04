# Nova — Release Readiness

Status as of Phase 8.5: **v1.0.0-stable**, core platform complete, not
yet published. This doc is the checklist for what shipping a real
build to the Play Store requires, and an honest record of what's
deliberately deferred versus what's actually done.

## Versioning

- `versionCode` — plain integer, increments by 1 on every release build
  that reaches a real device (internal test tracks included). Never
  reused, never decreases. Currently `2` (bumped from `1` in Phase 8.5
  for the `MIGRATION_2_3` schema change).
- `versionName` — `MAJOR.MINOR.PATCH[-rcN]`. Currently `1.0.0-stable`
  — Phase 8's `1.0.0-rc1` was promoted once the CRUD and persistence
  gaps that made it a release *candidate* rather than a release were
  closed (see `PROJECT_STATUS.md`). A future breaking change bumps to
  `1.1.0` or `2.0.0` per normal semver judgment; this isn't a
  strict policy document for that.

## Signing

Release builds are signed via `app/keystore.properties`, which is
gitignored and never committed. To build a real signed release locally:

1. Generate a keystore once (see `app/keystore.properties.example` for
   the exact `keytool` command). Store it somewhere durable and back it
   up — losing it means no future update can be signed to match the
   existing Play Store listing.
2. Copy `app/keystore.properties.example` to `app/keystore.properties`
   and fill in the real values.
3. `./gradlew :app:assembleRelease` or `:app:bundleRelease`.

Without `keystore.properties` present, `assembleRelease` still succeeds
and produces an **unsigned** APK — this is intentional so the build
stays green for anyone without access to the real release key.

## Pre-release checklist

- [ ] `./gradlew test` — full unit test suite passes (`core:domain`'s
      use cases, `Money`, `SavingsGoal.forecast()`, `AssistantInsightEngine`).
- [ ] `./gradlew :app:assembleRelease` builds and installs cleanly with
      `isMinifyEnabled` / `isShrinkResources` on — this is the build
      that actually ships, and it's the one build type that regularly
      finds ProGuard/R8 keep-rule gaps unit tests can't.
- [ ] Manually smoke-test the release build on a physical device: add an
      account, add a transaction in every category, set a budget, create
      a goal, ask the Assistant a few questions, kill and relaunch the
      app to confirm Room data persists.
- [ ] `./gradlew :benchmark:connectedBenchmarkAndroidTest` on a physical
      device; if the cold-start number regressed meaningfully from the
      last release, investigate before shipping.
- [ ] Bump `versionCode` and `versionName`.
- [ ] Tag the release commit.

## Deliberate deferrals

These are explicit product/infra decisions still open, not oversights —
each needs a decision from whoever owns the release, not a default:

- **Crash reporting.** Nova has no crash/ANR reporting pipeline (no
  Firebase Crashlytics or equivalent) yet. Adding one is a real vendor
  and privacy-disclosure decision (what gets sent off-device, what the
  Play Store data-safety form needs to say) — it shouldn't be silently
  wired in without that call being made explicitly.
- **Baseline Profile.** `:benchmark` can generate one (see
  `StartupBenchmark.kt`), but the profile itself has to come from a real
  run on physical hardware, not be hand-written. `app/src/main/baseline-prof.txt`
  doesn't exist yet — generate it and commit it once available; the
  `profileinstaller` dependency is already wired in to pick it up
  automatically.
- **Store listing assets.** Screenshots, feature graphic, and the actual
  Play Store description copy aren't part of this repo and need real
  product/marketing input, not placeholder text.
- **Privacy policy URL.** Required by Play Console even though Nova
  collects nothing off-device today (no network calls, no analytics) —
  still needs a real hosted page saying so before submission.

## What's already covered

- No `INTERNET` permission or any other permission declared — Nova has
  no network calls in v1, so there's nothing to request.
- `allowBackup="false"` — no cloud backup or device-to-device transfer
  of local financial data.
- Predictive back gesture support enabled (`enableOnBackInvokedCallback`).
- R8/ProGuard rules audited and scoped — the broad blanket keep-rule
  that shipped in earlier phases (keeping every synthetic method across
  the whole app) has been removed; it was quietly undermining the
  minification Phase 7 turned on.
- Room schema migrations are explicit (no destructive fallback) — see
  `MIGRATION_1_2` in `DatabaseModule`.
