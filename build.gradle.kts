plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false

    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.hilt.plugin) apply false
    alias(libs.plugins.detekt) apply true
}
// Applied at the root so `./gradlew detekt` runs across every module in one
// pass (matching how `./gradlew test`/`lint` already work project-wide)
// rather than needing a per-module task invocation in CI.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        // Fails the build on any finding at or above this severity —
        // matches the Phase 11.5.7 brief's "Build must fail on: ...
        // Lint failures" requirement, applied the same way to Detekt.
        ignoreFailures = false
    }
}
