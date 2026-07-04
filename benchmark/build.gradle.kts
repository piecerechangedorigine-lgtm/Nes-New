plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.novafinance.benchmark"
    compileSdk = 35

    defaultConfig {
        // Macrobenchmark's StartupTimingMetric relies on framework hooks
        // only available from API 29 (Android 10) onward.
        minSdk = 29
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Required for a com.android.test module to instrument another app
    // module's process rather than only its own.
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // Must match the app module's "benchmark" build type by name so
        // this module's single variant targets it automatically.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.junit)
    implementation(libs.uiautomator)
    implementation(libs.benchmark.macro.junit4)
}
