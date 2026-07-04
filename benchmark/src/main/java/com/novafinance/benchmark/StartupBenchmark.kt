package com.novafinance.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold-start time for Nova's single Activity — "how long until
 * the Dashboard is visible and interactive," the number the app's real
 * startup performance is judged on, not a synthetic proxy for it.
 *
 * Run on a physical device (Macrobenchmark results on an emulator aren't
 * representative — no real CPU throttling/scheduling behavior):
 *
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * This intentionally does not ship a generated app/src/main/baseline-prof.txt
 * alongside it. A Baseline Profile has to come from an actual run against
 * real hardware; hand-writing one here would be exactly the kind of fake
 * data this project's engineering rules rule out. Once a real run
 * produces one, drop it into app/src/main/baseline-prof.txt — the
 * profileinstaller dependency already wired into :app picks it up
 * automatically on the next release build, no other code change needed.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = "com.novafinance.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}
