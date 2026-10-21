# SellerChat

**Message any customer on WhatsApp or WhatsApp Business — with your price and order reply already typed.**

One screen: paste a number → pick WhatsApp / WhatsApp Business → the chat opens with your chosen template already in the message box. Plus labeled recents, reusable templates in English / اردو / Roman Urdu, a wa.me link + QR generator, and a one-tap "share my link to my status".

Built for small online sellers in Pakistan, India, Bangladesh, the UAE and Nigeria. **Zero runtime permissions. No account, no cloud, no backend. All data stays on the phone.** One AdMob banner on the Home screen only — never in the chat flow.

Package: `com.fahad1005.sellerchat` · Min SDK 24 · Target SDK 35 · Kotlin + Jetpack Compose (Material 3)

---

## Build a release AAB

You need JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).

```bash
# Debug APK (installable test build)
gradle :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release AAB for Google Play (unsigned until you add your keystore)
gradle :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab

# Release APK (what CI builds to measure the size)
gradle :app:assembleRelease
```

**Signing:** the release build is unsigned by default. For a real store upload, create your own keystore and either (a) add a `signingConfigs` block in `app/build.gradle.kts`, or (b) enroll in Play App Signing and sign the AAB with your upload key. In CI, GitHub Actions generates a *throwaway* keystore (env var `SELLERCHAT_DEBUG_KEYSTORE`) purely so the release APK is installable for size measurement — never ship that file to users.

No Gradle wrapper is committed (the build machines here have no Java to generate one); CI installs Gradle 8.9 via `gradle/actions/setup-gradle`. With Android Studio, just open the project root and it will offer to create the wrapper / sync as usual.

## Swap the AdMob IDs — TODO(FAHAD)

The app currently uses Google's public **sample** AdMob IDs (test ads only, zero revenue). Replace both before any real release:

1. **App ID** — in `app/src/main/AndroidManifest.xml`, the meta-data `com.google.android.gms.ads.APPLICATION_ID` (currently `ca-app-pub-3940256099942544~3347511713`).
2. **Banner unit ID** — in `app/src/main/java/com/fahad1005/sellerchat/ui/components/AdBanner.kt`, `SAMPLE_BANNER_AD_UNIT_ID` (currently `ca-app-pub-3940256099942544/6300978111`).

There is exactly one ad: a banner pinned to the bottom of the Home screen. There are no interstitial, app-open, or rewarded ads anywhere, by design.

## Edit the seed templates

The 12 first-launch templates live in one file:

`app/src/main/java/com/fahad1005/sellerchat/data/SeedData.kt`

Edit the `title` / `body` / `language` of any entry, or add new ones (keep `sortOrder` increasing). Placeholders `{name}`, `{price}`, `{item}`, `{qty}` are substituted when a chat is opened — `{name}` becomes the customer label, the rest are blanked so a raw placeholder never reaches a customer. Note: seeds are inserted only when the templates table is empty (first install); changing seeds does not rewrite templates on phones that already ran the app — users can edit/delete every seeded template in the app itself.

## Verify the size budget (release < 5 MB)

The release budget is a hard product constraint. CI prints a size report on every build (Actions → the run → "Report APK sizes"). To verify locally:

```bash
gradle :app:assembleRelease
ls -la app/build/outputs/apk/release/app-release.apk   # must stay under 5,000,000 bytes
```

How the budget is kept — every dependency justified:

| Dependency | Why it is allowed in |
|---|---|
| Jetpack Compose + Material 3 (BOM 2024.12.01) | The UI itself. R8 full mode + resource shrinking strip everything unused. |
| material-icons-**core** only | The *-extended* icon pack is megabytes of vectors. Missing glyphs (QR, chat bubble) ship as two tiny local vector drawables instead. |
| Room (runtime + ktx + compiler) | Templates + recents need ordered, searchable, durable local storage; Room is the smallest *safe* way to do that in Kotlin. |
| play-services-ads 23.6.0 | The single Home banner. The largest single line item; accepted because ads are the v1 business model. No other Google services (no Firebase, no Analytics) are included. |
| ZXing **core** 3.5.3 | QR *generation* only (~500 KB pre-shrink, much less after R8). The android-embedded/camera variants were rejected — no scanning is needed. |
| core-ktx, activity-compose, lifecycle | Small AndroidX glue required by Compose/ViewModel. |
| Nothing else | No DI framework, no navigation library (in-app state routing), no image loaders, no networking stack, no custom fonts, no DataStore (SharedPreferences covers settings at zero dependency cost). |

Release config doing the work: `isMinifyEnabled = true` (R8 full mode via `android.enableR8.fullMode=true`), `isShrinkResources = true`, vector drawables only, system fonts only.

## Project layout

```
app/src/main/java/com/fahad1005/sellerchat/
├── MainActivity.kt          — entry point, ACTION_SEND pre-fill
├── SellerChatApp.kt         — Application, AdMob init
├── data/                    — Room entities/DAOs/database, seed templates,
│                              country list, SharedPreferences settings
├── util/                    — phone normalization, QR generation, time
├── viewmodel/               — MainViewModel (MVVM, no DI framework)
└── ui/                      — Compose screens in spec order:
                               Home, Recents, Templates, QR, ShareStatus,
                               Settings, FirstRun + theme/components
```

Privacy: see `PRIVACY_POLICY.md`. Store listing draft: see `STORE_LISTING.md`. Build decisions: see `BUILD_NOTES.md`.
