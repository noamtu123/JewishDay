import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The calendar and zmanim logic, as plain Kotlin on the JVM: no Android SDK on the classpath, so
// nothing here can reach for a Context, a resource or a widget — that is the app's side. What this
// module works out is worded by the app (see :app's i18n package).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    // Everything the app may use is marked public on purpose; the rest stays internal.
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

lint {
    abortOnError = true
    warningsAsErrors = true
}

dependencies {
    // KosherJava's calendar types appear in this module's API (the app builds JewishCalendars too).
    api(libs.kosherjava.zmanim)
    // Only the @Immutable/@Stable annotations — no Compose runtime, no Android — so the app's Compose
    // compiler can trust these types without the module depending on Compose itself.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime.annotation)

    testImplementation(libs.junit)
}
