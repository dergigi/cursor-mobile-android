import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Release signing values: prefer CI env vars, fall back to local.properties
// (which Gradle does not expose as project properties by default).
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(envName: String, propName: String): String? =
    System.getenv(envName) ?: localProps.getProperty(propName)

android {
    namespace = "com.cursor.mobile"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.cursor.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingValue("KEYSTORE_PATH", "RELEASE_STORE_FILE")
            val keystorePassword = signingValue("KEYSTORE_PASSWORD", "RELEASE_STORE_PASSWORD")
            val keyAlias = signingValue("KEY_ALIAS", "RELEASE_KEY_ALIAS")
            val keyPassword = signingValue("KEY_PASSWORD", "RELEASE_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() &&
                !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
            ) {
                // Resolve relative to the repo root, not the app module, so CI
                // (which writes release.keystore at the workspace root) is signed
                // with the real keystore instead of silently falling back to debug.
                storeFile = rootProject.file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs["release"].takeIf {
                it.storeFile?.exists() == true
            } ?: run {
                // If CI intended to sign (KEYSTORE_PATH set) but the keystore isn't
                // resolvable, fail loudly instead of shipping a debug-signed release.
                if (!System.getenv("KEYSTORE_PATH").isNullOrBlank()) {
                    throw GradleException(
                        "Release keystore not found (KEYSTORE_PATH=${System.getenv("KEYSTORE_PATH")}); " +
                            "refusing to fall back to the debug key for a release build."
                    )
                }
                signingConfigs["debug"]
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // WorkManager + Hilt
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Ktor (HTTP client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // OkHttp SSE
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)

    // Socket.io (CursorRemote relay)
    implementation(libs.socket.io.client)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Image loading
    implementation(libs.coil.compose)

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Biometric
    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.lifecycle.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
