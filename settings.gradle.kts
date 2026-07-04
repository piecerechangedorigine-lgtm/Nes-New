pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Nova"

include(":app")

include(":core:designsystem")
include(":core:data")
include(":core:domain")
include(":core:navigation")

include(":feature:dashboard")
include(":feature:accounts")
include(":feature:transactions")
include(":feature:budgets")
include(":feature:goals")
include(":feature:analytics")
include(":feature:assistant")
include(":feature:profile")
include(":feature:debt")

