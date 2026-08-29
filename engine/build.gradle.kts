import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// The engine is deliberately plain Kotlin: no Android, no Compose, no coroutines.
// Everything here runs on the JVM in milliseconds, so the rules are testable
// without an emulator — and the module lifts to commonMain when iOS arrives.
kotlin {
    explicitApi()
    compilerOptions {
        // 17 because that is what the Android app consumes, not because of the
        // JDK this happens to be built on.
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}
