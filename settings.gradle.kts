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

rootProject.name = "manakich-beirut"
include(":engine")
include(":app")

// There is deliberately no root build.gradle.kts declaring plugins with `apply false`:
// that would make every build resolve the Android Gradle Plugin, including
// `gradle :engine:test`. Each module declares its own plugins from the version
// catalog instead, so the engine builds and tests with no Android toolchain at all.
