plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The Wear OS companion: a tile and a small app that mirror today's habits from the phone.
// It shares the phone's applicationId and signing key, which the Data Layer requires.
android {
    namespace = "app.tsuzuku.wear"
    compileSdk = 36

    androidResources {
        localeFilters += listOf("en", "tr", "es", "de", "pt-rBR", "ja", "fr", "it", "ru", "ko")
    }

    defaultConfig {
        applicationId = "app.tsuzuku"
        minSdk = 30
        targetSdk = 36
        versionCode = 30
        versionName = "2.8.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.activity:activity-compose:1.12.1")
    implementation("androidx.wear.compose:compose-material3:1.6.1")
    implementation("androidx.wear.compose:compose-foundation:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.wear.tiles:tiles:1.6.2")
    implementation("androidx.wear.protolayout:protolayout:1.4.2")
    implementation("androidx.wear.protolayout:protolayout-material3:1.4.2")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.3.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20251224")
    // JVM screenshot rendering of the watch screens (no emulator needed)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(platform("androidx.compose:compose-bom:2026.06.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // Debug only: lets the preview test inflate the tile like a watch would.
    debugImplementation("androidx.wear.tiles:tiles-renderer:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Watch previews render on request only:
//   ./gradlew :wear:testDebugUnitTest -Ppreviews --tests "*WatchPreviewTest*"
tasks.withType<Test>().configureEach {
    if (!project.hasProperty("previews")) {
        exclude("**/WatchPreviewTest*")
    }
    systemProperty("previews.dir", rootProject.layout.projectDirectory.dir("previews/images").asFile.absolutePath)
    maxHeapSize = "2g"
    jvmArgs("-Duser.language=en", "-Duser.country=US", "--add-opens=java.base/java.nio=ALL-UNNAMED")
}
