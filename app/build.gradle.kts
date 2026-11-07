plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    androidResources {
        // Ship only the languages the app is fully translated into.
        localeFilters += listOf("en", "tr")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    namespace = "app.tsuzuku"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.tsuzuku"
        minSdk = 26
        targetSdk = 36
        versionCode = 27
        versionName = "2.5.0"

        buildConfigField("boolean", "FIREBASE_CONFIGURED", hasFirebaseConfig.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            // Optimised, non-debuggable build: Compose runs several times faster than in debug.
            isMinifyEnabled = true
            isShrinkResources = true
            // Signed with the same key as earlier releases so updates install in place and
            // Google sign-in (registered with this key's SHA-1) keeps working.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

kotlin {
    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.1")
    // Compiles the libraries' bundled baseline profiles at install, so the first launches are fast too.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    // 1.5 brings the Material 3 Expressive APIs (shapes, motion, wavy progress, loading indicator).
    implementation("androidx.compose.material3:material3:1.5.0-alpha18")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // Sends today's habits to the Wear OS companion and receives taps from its tile.
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("androidx.health.connect:connect-client:1.1.0")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    // JVM screenshot rendering for the preview gallery (no emulator needed)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.json:json:20251224")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// The preview gallery renders screenshots on the JVM. It is slow, so it only runs on request:
//   ./gradlew :app:testDebugUnitTest -Ppreviews --tests "app.tsuzuku.previews.*"
tasks.withType<Test>().configureEach {
    if (!project.hasProperty("previews")) {
        exclude("**/previews/**")
    }
    systemProperty("previews.dir", rootProject.layout.projectDirectory.dir("previews/images").asFile.absolutePath)
    maxHeapSize = "2g"
    // Robolectric native libs are looked up with a lowercased OS name; a Turkish default
    // locale turns "windows" into "wındows" and the lookup fails.
    jvmArgs("-Duser.language=en", "-Duser.country=US")
}
