import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.plugin)
}

// Release signing reads from app/keystore.properties, which is gitignored
// and never committed — see keystore.properties.example for the expected
// format. Missing file (a fresh checkout, CI without the secret, a
// contributor without release access) is a normal case, not an error:
// the release build type simply stays unsigned rather than failing the
// build, since most day-to-day work never needs a signed release APK.
val keystorePropertiesFile = rootProject.file("app/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigningConfig = keystorePropertiesFile.exists()

android {
    namespace = "com.novafinance.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.novafinance.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.3.5-foundation-complete"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        // Release-shaped build for Macrobenchmark: same optimization
        // profile as release (minified, non-debuggable) but debug-signed
        // so :benchmark can install and instrument it without needing the
        // real release key.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))

    implementation(project(":feature:dashboard"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:transactions"))
    implementation(project(":feature:budgets"))
    implementation(project(":feature:goals"))
    implementation(project(":feature:analytics"))
    implementation(project(":feature:assistant"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:debt"))

    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.profileinstaller)
    implementation(libs.glance.appwidget)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    implementation(libs.work.runtime.ktx)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)
}
