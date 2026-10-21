// SellerChat app module.
// Size strategy (release MUST stay under 5 MB): R8 full mode + shrinkResources,
// vector drawables only, system fonts, a deliberately short dependency list
// (see README.md for the per-dependency justification).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// CI-only signing: when GitHub Actions builds the release APK it generates a
// throwaway keystore and passes its path via this env var, purely so the APK
// is installable for size measurement. A real release uses Fahad's own
// keystore / Play App Signing — never this one.
val ciKeystore: String? = System.getenv("SELLERCHAT_DEBUG_KEYSTORE")

android {
    namespace = "com.fahad1005.sellerchat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fahad1005.sellerchat"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        if (ciKeystore != null) {
            create("ciDebug") {
                storeFile = file(ciKeystore)
                storePassword = "android"
                keyAlias = "ci"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ciKeystore != null) {
                signingConfig = signingConfigs.getByName("ciDebug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            // Strip unused locale/ABI metadata blobs that libraries drag in.
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Local storage for templates + recents. Room is justified over raw SQLite:
    // compile-checked queries, Flow integration, and reorder/search support.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Ads: exactly ONE banner on the Home screen. Sample IDs until Fahad adds
    // his own (see README). TODO(FAHAD): replace with real AdMob IDs.
    implementation(libs.play.services.ads)

    // QR generation: ZXing core only (the encoder). No zxing-android-embedded,
    // no camera/barcode scanning — generation is all this app needs.
    implementation(libs.zxing.core)
}
