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

// Signing (v2.1): TWO modes, picked by which env vars the CI workflow sets.
//  1. PERMANENT (preferred): the workflow decodes the founder's real
//     keystore from the SIGNING_KEYSTORE_B64 repo secret and passes its
//     path + passwords via SELLERCHAT_SIGNING_* — alias is always
//     "sellerchat". The keystore is a PKCS12 file (sellerchat.p12,
//     generated locally by the lead agent; the file itself never enters
//     this repo), so the config below pins storeType "PKCS12" rather
//     than relying on the JDK default. Every build signed this way
//     installs OVER the previous one and app data survives updates.
//  2. THROWAWAY FALLBACK: if the secret is not set (yet), CI generates a
//     one-off keystore (SELLERCHAT_DEBUG_KEYSTORE) purely so the APK is
//     installable for size measurement. Builds in this mode can NOT
//     update a permanent-signed install, or each other. The workflow
//     prints "SIGNING: FIXED KEY" vs "SIGNING: THROWAWAY FALLBACK
//     (secrets not set)" on the run page so the mode is never a guess.
// No keystore or password is ever committed to this repo.
val ciKeystore: String? = System.getenv("SELLERCHAT_DEBUG_KEYSTORE")
val fixedKeystore: String? = System.getenv("SELLERCHAT_SIGNING_KEYSTORE")
val fixedStorePassword: String? = System.getenv("SELLERCHAT_SIGNING_STORE_PASSWORD")
val fixedKeyPassword: String? = System.getenv("SELLERCHAT_SIGNING_KEY_PASSWORD")

android {
    namespace = "com.fahad1005.sellerchat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fahad1005.sellerchat"
        minSdk = 24
        targetSdk = 36
        // NOTE: versionCode history — v1.0.x = 1–3, v2.0 = 4,
        // v2.1 = 5, v2.2 = 6, v2.3 = 7, v2.4 = 8, v2.4.1 = 9,
        // v2.5 = 10 — so v2.6 is 11 (Android refuses a lower or
        // equal code on update).
        versionCode = 11
        versionName = "2.6"
    }

    signingConfigs {
        if (fixedKeystore != null) {
            create("permanent") {
                storeFile = file(fixedKeystore)
                storeType = "PKCS12"
                storePassword = fixedStorePassword
                keyAlias = "sellerchat"
                keyPassword = fixedKeyPassword
            }
        }
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
            if (fixedKeystore != null) {
                signingConfig = signingConfigs.getByName("permanent")
            } else if (ciKeystore != null) {
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

    // Follow-up reminders (v2 spec §0/§4): WorkManager is the sanctioned
    // scheduling cost — a reliable 2-day reminder without any permission
    // beyond the in-context POST_NOTIFICATIONS ask.
    implementation(libs.work.runtime.ktx)

    // Ads: exactly ONE banner on the Home screen. Sample IDs until Fahad adds
    // his own (see README). TODO(FAHAD): replace with real AdMob IDs.
    implementation(libs.play.services.ads)

    // QR generation: ZXing core only (the encoder). No zxing-android-embedded,
    // no camera/barcode scanning — generation is all this app needs.
    implementation(libs.zxing.core)
}
