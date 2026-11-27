import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
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
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.noamtu.jewishday"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "1.3.0"

        val locales = translationLocales(file("src/main/res"))
        buildConfigField("String[]", "APP_LOCALES", locales.joinToString(prefix = "{", postfix = "}") { "\"$it\"" })
    }

    androidResources {
        // res/xml/locales_config is generated from the translation folders (and resources.properties),
        // so Android 13+'s per-app language setting always lists exactly the languages the app has.
        generateLocaleConfig = true
        // Ship only those languages: the libraries' own translations into dozens of others would
        // never be shown, since the app itself speaks only these.
        localeFilters += resourceLocaleFilters(translationLocales(file("src/main/res")))
    }

    // Release is the only variant shipped, so it is the one tested. Since AGP 9, unit tests exist
    // only for the tested build type, so this is what keeps :app:testReleaseUnitTest.
    testBuildType = "release"

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
        // Lint is run explicitly as :app:lintRelease (see the justfile); this only stops it being
        // re-run as part of assembleRelease, which would analyze the same variant twice.
        checkReleaseBuilds = false
        // :app:lintRelease also lints :core, so one task covers every module.
        checkDependencies = true
        // targetSdk stays at 36 on purpose until the glass widgets' sky bitmaps are measured against
        // API 37's RemoteViews memory cap on a device; kept visible as a note rather than a warning.
        informational += "OldTargetApi"
        // "A newer version is available" depends on what was published today, not on this code: with
        // warnings as errors it would fail a build that passed yesterday. Upgrades are a deliberate
        // step (coroutines, for one, stay on the version AndroidX brings in).
        informational += listOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
        // Lint has nothing left to say; keep it that way. A warning that truly cannot be fixed is
        // suppressed where it occurs, with the reason beside it.
        warningsAsErrors = true
    }

    bundle {
        // The app switches its own language at runtime, so an app bundle must carry every language
        // to every device rather than only the phone's. (Releases are APKs; this keeps a bundle honest.)
        language {
            enableSplit = false
        }
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
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    // Named here because the manifest configures its InitializationProvider (for ProfileInstaller).
    implementation(libs.androidx.startup.runtime)
    implementation(libs.hilt.android)
    implementation(project(":core"))

    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    // android.jar's org.json is a stub in unit tests; the real one lets release parsing be tested.
    testImplementation(libs.json)
    // Dispatchers.setMain, for the view models' viewModelScope.
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

/**
 * The app's languages, as BCP 47 tags: English (res/values) first, then one per translation folder
 * (res/values-<language>[-r<REGION>] or values-b+…) holding a strings.xml. The in-app language
 * picker lists these, so adding a translation folder is all it takes for a language to appear there;
 * the system's own per-app language list comes from the generated locale config below.
 */
fun translationLocales(res: File): List<String> {
    val qualifier = Regex("""values-(?:b\+([A-Za-z0-9+]+)|([a-z]{2,3})(?:-r([A-Z]{2}))?)""")
    // Hebrew's folder keeps the old "iw" code every Android version reads; the tag is "he".
    val modern = mapOf("iw" to "he", "in" to "id", "ji" to "yi")
    val found = res.listFiles().orEmpty()
        .filter { it.isDirectory && File(it, "strings.xml").isFile }
        .mapNotNull { qualifier.matchEntire(it.name) }
        .map { match ->
            val (bcp, language, region) = match.destructured
            if (bcp.isNotEmpty()) {
                bcp.split('+').joinToString("-")
            } else {
                listOf(modern[language] ?: language, region).filter(String::isNotEmpty).joinToString("-")
            }
        }
        .sorted()
    return listOf("en") + found
}

/**
 * The resource-qualifier forms of [tags] for `localeFilters`: "en", "iw" and "he" for Hebrew (its
 * folders use the old code, and some libraries the new one), "b+pt+BR" for a regional tag.
 */
fun resourceLocaleFilters(tags: List<String>): List<String> {
    val legacy = mapOf("he" to "iw", "id" to "in", "yi" to "ji")
    return tags.flatMap { tag ->
        if ('-' in tag) listOf("b+" + tag.replace('-', '+')) else listOfNotNull(tag, legacy[tag])
    }.distinct()
}
