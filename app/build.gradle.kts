import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Load signing credentials from a gitignored keystore.properties (kept out of VCS).
// If the file is absent (e.g. a clean checkout without secrets), the release build
// simply stays unsigned instead of failing the configuration.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.noamtu.jewishday"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.noamtu.jewishday"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "0.6.1"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    lint {
        abortOnError = true
        // Lint runs explicitly via :app:lintDebug; don't re-run it during release assembly.
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    implementation(libs.kosherjava.zmanim)

    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Android lint can analyze generated Hilt sources while release compilation is still creating
// them when `lintDebug` and `assembleRelease` are requested together. Keep the combined command
// reliable by running debug lint after release assembly in that task graph.
tasks.matching { it.name in DebugLintTaskNames }.configureEach {
    mustRunAfter("assembleRelease")
}

val DebugLintTaskNames = setOf(
    "lintAnalyzeDebug",
    "lintAnalyzeDebugAndroidTest",
    "lintAnalyzeDebugUnitTest",
    "lintReportDebug",
    "lintDebug",
)
