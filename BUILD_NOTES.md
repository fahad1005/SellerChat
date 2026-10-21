# SellerChat — Build Notes

## Architecture decisions

- **Single module, MVVM, no DI framework.** One `MainViewModel` (AndroidViewModel) owns all state; screens are stateless Compose functions reading its flows. A hand-written `ViewModelProvider.Factory` replaces Hilt/Koin — for a 7-screen utility, a DI framework costs size and complexity and buys nothing.
- **No navigation library.** Routing is a small sealed `Route` state in the root composable (Main tabs / QR / ShareStatus / Settings). Zero extra dependency, instant transitions, nothing to leak.
- **Room for templates + recents, SharedPreferences for settings.** Room earns its place: templates need ordering + reorder persistence, recents need search-as-you-type and upsert-by-number (the duplicate rule). Settings are five scalars — DataStore would be a bigger dependency for the same job, so a typed `SettingsStore` wraps SharedPreferences.
- **WhatsApp launching = wa.me intents aimed at a package.** `Intent(ACTION_VIEW, https://wa.me/<e164>?text=…)` with `setPackage(com.whatsapp | com.whatsapp.w4b)`. The `?text=` parameter is WhatsApp's documented pre-fill: text lands in the message box, never auto-sent. Installed-app detection uses `getPackageInfo` + the manifest `<queries>` block (package visibility, **not** a permission). There is deliberately **no** number-existence check anywhere in the codebase.
- **Default country without permissions.** `TelephonyManager.getSimCountryIso()` needs no permission; fallback chain is SIM → device locale → Pakistan (primary market). The user's manual pick is persisted and always wins afterwards.
- **Clipboard discipline.** Read in exactly one place: the Home paste button's click handler — foreground, user-initiated, per spec §6.
- **QR via ZXing core only.** Generation needs just `QRCodeWriter` (EC level M, margin hint = quiet zone, black on white, no logo). PNG sharing goes through a cache-dir `FileProvider` limited to `cache/qr/`.
- **Ads containment.** `AdView` exists in exactly one composable (`AdBanner`), placed once at the bottom of Home. No interstitial/app-open/rewarded classes are imported anywhere in the project (grep-verifiable).

## Size-budget strategy (release < 5 MB)

1. R8 **full mode** + `shrinkResources` on release.
2. Dependency diet — see the table in README.md. The two big calls: material-icons-**core** (not extended; missing glyphs are 2 local vectors) and ZXing **core** (not the embedded/camera artifact). No Firebase, no Analytics, no image loader, no networking stack, no custom fonts (system font renders Urdu fine), vector drawables only.
3. Packaging excludes strip library license/notice blobs.
4. CI builds the release APK on every push and prints its byte size ("Report APK sizes" step), so a budget regression is visible the commit it happens.

## Spec interpretations (simplest option chosen, per instructions)

- **Seed template titles for items 7–12:** the spec gives titles only for the 6 English templates. Urdu-script seeds got matching Urdu titles (قیمت کا جواب / آرڈر کنفرم / ادائیگی کی تفصیل); Roman Urdu seeds got the English title + " (Roman)". All *bodies* are byte-verbatim from the spec.
- **Template drag-reorder** is active in the **All** language tab. Reordering inside a filtered subset would silently scramble the global order; in a filter the list is read-only order. (Reorder is a spec feature; the All tab is where the full list lives.)
- **"My WhatsApp number"** lives on the Share-to-Status screen itself (shown as a one-time inline form when unset), stored in settings — the spec's suggested simplest option.
- **Default app setting** also seeds Home's initial app selection (explicit per-session switching on Home overrides it until the app restarts).
- **No Gradle wrapper jar** is committed: the authoring machine has no Java/Gradle. CI installs Gradle 8.9 via setup-gradle (same pattern as the sibling StatusSaver repo), and `gradle-wrapper.properties` is present for local Android Studio use.

## SIZE + ACCEPTANCE: verified at CI

> **Measured release APK size: _to be filled in from GitHub Build #1_** ("Report APK sizes" step in the Actions log). Budget: **< 5,000,000 bytes**. This file ships without a fabricated number — the CI log is the source of truth, and the release pipeline is configured (R8 full mode, shrinkResources, dependency diet above) to land under budget.

| # | Acceptance criterion | How the code satisfies it | Where CI/you verify |
|---|---|---|---|
| 1 | Release build under 5 MB | R8 full mode + shrinkResources + dependency diet | Actions log, "Report APK sizes" |
| 2 | Zero runtime permissions declared/requested | Manifest contains no `<uses-permission>` (grep proof in the delivery report); no `requestPermissions` call exists | `grep -r uses-permission app/src/main/AndroidManifest.xml` → no matches |
| 3 | Open → WhatsApp chat in < 5 s | One intent, no intermediate screens; the Recents write happens asynchronously *after* launch | Phone test with a stopwatch |
| 4 | Template text lands in the message box, never auto-sent | wa.me `?text=` pre-fill only; no send automation exists | Phone test: text present, unsent |
| 5 | WhatsApp **and** WhatsApp Business paths work | Package-aimed intents + `<queries>` detection + failure dialog offering the other app | Phone test on a dual-app phone |
| 6 | Never shows a "number not on WhatsApp" pre-check | No such check is implemented, anywhere | Code grep + phone test with an unregistered number |
| 7 | No interstitial in the core flow | Only an `AdView` banner composable exists; no InterstitialAd/AppOpenAd/RewardedAd imports | Code grep |
| 8 | Recents/templates/labels survive restart + update | Room database file + SharedPreferences; no in-memory-only state | Phone test: kill app, relaunch, update install |
| 9 | Urdu + Roman Urdu templates display correctly | Urdu bodies render inside an explicit RTL layout-direction provider in lists, previews and the editor | Screenshot check |
| 10 | Shared text with a number pre-fills Home | `ACTION_SEND` intent-filter + digit-run extraction in `PhoneUtils.extractNumber` | Phone test: Share a number from any app → SellerChat |
| 11 | Works in airplane mode (except the ad) | Every feature is local; the only network user in the app is the AdMob banner | Phone test with airplane mode on |
