# SellerChat — Build Notes

## v2.1 (polish + signing permanence — built on v2.0, no new screens/tabs/settings)

**versionCode 5 / versionName "2.1".** Two halves: the founder's six UI fixes (Part A) and permanent signing (Part B). Plus a founder addendum on signing visibility and multi-device testing.

### Part A — the six fixes, task by task

**Task 1 — Keyboard overlap.** The manifest already carried `android:windowSoftInputMode="adjustResize"` on MainActivity from v2 (verified, unchanged — so the manifest is NOT in this update zip). The real defect was layout: Home was one non-scrolling `Column`, so a shrunken viewport overflowed and pushed the CTA (and the banner) off the bottom edge. Fix:
- Home is now `Column { scrollable content (weight 1, verticalScroll + imePadding) ; banner }` — the "Open chat" CTA lives inside the scroll region and is always reachable by scrolling while typing.
- MainScreen's Scaffold now **hides the bottom NavigationBar while the IME is visible** (`WindowInsets.ime` bottom > 0), so the nav can never float above the keyboard; it returns when the keyboard closes.
- Layout math: viewport height (after adjustResize + Scaffold insets) = scroll region + banner strip, exactly. The banner is a sibling AFTER the scroll region, so content length can never displace it, and it can never overlap the CTA.

**Task 2 — Banner restored.** Root cause, plainly: **the banner was never deleted in v2** — `AdBanner()` was still called once, as the last child of Home's non-scrolling Column, behind a `Spacer(Modifier.weight(1f))`. On any viewport too short for the full content (smaller phones, or the keyboard-shrunk window), that last child is the first thing laid out past the bottom edge — invisible in practice, which is exactly what the founder saw. v2.1 pins it as a fixed strip under the scroll area, directly above the bottom navigation — v1.0's position. `AdView` still exists in exactly one composable (`ui/components/AdBanner.kt`), called from exactly one place.

**Task 3 — Template preview + smart auto-fill.** One shared function, `MainViewModel.filledTemplateText(template)`, is now the ONLY fill implementation (the old `PhoneUtils.applyTemplate` was deleted; both of its call sites migrated). It feeds: the Home inline preview, Open chat, Copy wa.me link, Show QR, and Try Demo — so the preview is byte-for-byte what gets sent.
- Label parsing: split on `—`, `–`, ` - `, or `,` → segment 1 = `{name}`, segment 2 = `{item}` ("Ayesha — kurta" → Ayesha / kurta); no separator → the whole label is `{name}`, `{item}` blank. `{price}` / `{qty}` blanked as before.
- After the four known replacements, **any leftover `{...}` token is regex-stripped** (custom placeholders, typos, half-typed tokens) and double spaces collapsed — a raw bracket can never reach a customer.
- The preview sits directly under the chip row: the selected template's FILLED text, max 2 lines (material3 `Text` ellipsizes at maxLines). It re-renders live as the label is typed. The v1 long-press bottom sheet still shows the RAW body (placeholders visible) with Edit — that is the editor-facing view; the inline preview is the customer-facing one. Recents tap-to-reopen still sends no template (unchanged), and a sent chat still records the template TITLE on the Recent.

**Task 4 — Home chip ordering.** Chips sort by a priority language list, stable within groups: the Settings Preferred Template Language if set; otherwise derived from the user's default country —

| Default country | Priority languages (first → last) |
|---|---|
| PK | EN, UR, ROMAN_UR |
| IN | HI, EN |
| BD | EN |
| AE / NG | EN |
| any other | EN |

(languages outside the list follow in library order). The **"No template" chip moved to the far END** of the chip row, after every template; it remains selectable per-send.

**Task 5 — Recents card cleanup.** The right-side stacked pill column is gone. New row: line 1 = label/number + **status dot + thin outline badge** beside the name (dot + border + text in the status color); line 2 = number · app · relative time; template used = small **plain text** under line 2 (no pill/background); memo still pinned under the name block; waiting clock still shows for INQUIRY; follow-up line unchanged. Status colors: INQUIRY orange `0xFFEF6C00` (the waiting clock's orange), ORDERED blue `0xFF1565C0`, PAID green `0xFF2E7D32`, SHIPPED purple `0xFF6A1B9A`. The badge stays the tap-to-cycle target with a 48dp-tall tap target around the small visual. Bottom sheet VERIFIED in code to contain exactly: **Open Chat / Follow-up in 2 days / Add Memo / Mark Paid** (the memo action reads "Edit Memo" when a memo already exists) — nothing was missing, nothing added.

**Task 6 — Size & font audit.** `res/font/` contains exactly ONE file: `noto_nastaliq_urdu.ttf`. Hindi/Marathi/Tamil/Telugu render in system fonts — `TemplateText.kt` references only `R.font.noto_nastaliq_urdu`, applied only to Urdu-script text; no other font file exists in the project. Nastaliq re-subset test (fonttools, same layout features as v2):
- Current cut: **899,288 bytes** (unicodes = Latin U+0020–007E + U+00A0 + U+0600–06FF + U+0750–077F; cmap = 356 entries). Verified: **all 53 distinct codepoints** in the Urdu seed strings (3 titles + 3 bodies) are present.
- Tighter trial cut (Arabic blocks + presentation forms U+FB50–FDFF/U+FE80–FEFF + digits + basic punctuation, Latin letters dropped): **887,320 bytes** — a saving of only **11,968 bytes**, AND it removes 13 Latin letters (a c d e h i m n o p r t y) that occur in the Urdu seed bodies inside the `{name}` / `{item}` / `{price}` / `[payment method]` tokens, failing the every-seed-codepoint rule.
- **Decision: keep the 899,288-byte cut.** Both keep-conditions were met (< 50 KB saving AND glyph risk). The trial file was discarded; the shipped font is byte-identical to v2.0's.

### Part B — permanent signing

- **Model:** the keystore lives ONLY as repo secrets: `SIGNING_KEYSTORE_B64` (base64 of the PKCS12 file `sellerchat.p12`), `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`. The key alias is fixed as `sellerchat` in the workflow/Gradle (not a secret). Nothing secret is ever committed or printed.
- **Key provenance:** the permanent key was generated LOCALLY by the lead agent as a PKCS12 file (`sellerchat.p12`, alias `sellerchat`, RSA 2048, 100-year self-signed cert) and handed to the founder as a base64 text file for ONE paste into the `SIGNING_KEYSTORE_B64` secret. The .p12 and its base64 stay OUT of this repo — never committed, never uploaded. The cert fingerprint is quoted to the founder in chat for his records. (An earlier design generated the key with a one-time in-GitHub workflow, `generate-signing-key.yml`; the founder found that multi-step ceremony too difficult, so the workflow was REMOVED from the package and that road no longer exists.)
- **build.yml:** a "Prepare signing" step decodes the secret to `$RUNNER_TEMP/sellerchat.p12` and hands Gradle the PATH via `$GITHUB_ENV` (passwords travel separately, as step env on the build step only — never written to disk). PKCS12 is safe end-to-end on the JDK 17 runner: it is the JDK's default keystore type, and the Gradle `permanent` signing config additionally pins `storeType = "PKCS12"` — no JKS assumption anywhere. If the secret is absent, the step generates the old throwaway runner key instead, so a build NEVER hard-fails before setup. `app/build.gradle.kts` mirrors this: a `permanent` signing config (env `SELLERCHAT_SIGNING_*`) wins over the `ciDebug` throwaway config when both exist.
- **Visibility (founder addendum 1):** every build prints exactly one of these lines in the "Prepare signing" step log, again in the "Report build sizes and signing mode" step, and in the GitHub job summary:
  - `SIGNING: FIXED KEY`
  - `SIGNING: THROWAWAY FALLBACK (secrets not set)`
  A build counts as "the fix" ONLY if its run page says FIXED KEY.
- **AAB too:** the build now also runs `:app:bundleRelease`, and the report step + job summary print BOTH the release APK and release AAB byte sizes (budget still < 5,000,000 bytes each; the exact figures come from CI, as always — nothing is fabricated here).

### Founder addendum 2 & 3 — testing, and what comes next

- UPDATE_INSTRUCTIONS.md now carries the **multi-device test protocol** (per phone: last uninstall → install the FIXED KEY v2.1 release APK → create one Recent → install the follow-up build OVER it → the Recent must still be there; repeat on phone 2).
- ⚠️ **PLANNED PROOF BUILD: v2.1.1** — a version-bump-only follow-up (same fixed key, no feature changes) will be packaged right after v2.1, solely so the founder can run the install-over proof on both phones. It is expected; it is not a bug fix.

### v2.1 acceptance additions (verified at CI / on phone — see UPDATE_INSTRUCTIONS.md)

| # | Criterion | How |
|---|---|---|
| 16 | Keyboard: nav hidden while typing, CTA reachable by scroll | Phone check in UPDATE_INSTRUCTIONS |
| 17 | Banner visible at the bottom of Home again | Phone screenshot |
| 18 | Preview = sent text; "Ayesha — kurta" fills name + item; no raw `{}` anywhere | Phone check |
| 19 | Chips: preferred/country language first; "No template" last | Phone check |
| 20 | Recents card: dot + outline badge, template as plain text | Phone screenshot |
| 21 | Signing: run page says `SIGNING: FIXED KEY` | Actions run page / job summary |
| 22 | Install-over proof: v2.1.1 over v2.1 keeps the Recent, on BOTH phones | Multi-device protocol |

## v2.0 (built on the v1 codebase — no architecture rewrite)

Worked in the founder's order: **(1) database schema, (2) UI files, (3) manifest + resources.**

### 1. Database schema — Room v1 → v2 (`data/Entities.kt`, `data/Migrations.kt`, `data/AppDatabase.kt`, `data/Daos.kt`)

Non-destructive `Migration(1, 2)` — ALTER-only, no `fallbackToDestructiveMigration` anywhere. v1 acceptance requires templates/recents/labels to survive the update.

**Template** — gains one column:
- `purpose` TEXT NOT NULL DEFAULT `'OTHER'` (enum `TemplatePurpose`: PRICE / ORDER / PAYMENT / DELIVERY / OTHER). Backfilled by exact seed title for the 12 v1 seeds (10 titled seeds map to PRICE/ORDER/PAYMENT/DELIVERY; "Out of stock" and "Thanks after order" stay OTHER; user-edited titles stay OTHER — no guessing). `language` already existed in v1; the enum extends EN / UR / ROMAN_UR → + HI / TA / TE / MR (stored v1 names untouched — renaming them would break existing rows).

**Recent** — gains the mini-CRM columns:
- `status` TEXT NOT NULL DEFAULT `'INQUIRY'` (`RecentStatus`: INQUIRY / ORDERED / PAID / SHIPPED)
- `memo` TEXT (nullable)
- `statusChangedAt` INTEGER NOT NULL DEFAULT 0 — **backfilled from each row's own `timestamp`**, so a migrated customer's waiting clock starts at their last message, not the epoch
- `followUpAt` INTEGER (nullable), `followUpNotified` INTEGER NOT NULL DEFAULT 0 (1 = real WorkManager notification scheduled, 0 = in-app reminder only)
- `paidAmountMinor` INTEGER (nullable), `paidAt` INTEGER (nullable)

Every ALTER default mirrors the entity `@ColumnInfo(defaultValue = …)` exactly so Room's post-migration schema validation passes. Schema version bumped 1 → 2; migration registered in the builder.

**Data-layer consequences handled in the same pass:**
- `MainViewModel.recordRecent` used to upsert a *fresh* `Recent` (REPLACE) on every re-message — that would have wiped the new CRM fields. It now loads the existing row (`RecentDao.getByNumber`) and merges, preserving status/memo/follow-up/payment; only brand-new numbers get the Settings default status.
- **Seeds v2 pass** (`SettingsStore.seedsV2Done` flag): on first v2 launch — fresh installs *and* upgraders — the 12 new templates (3 Hinglish + 3 Tamil + 3 Telugu + 3 Marathi) insert once, appended after the existing sort order. Never duplicated on relaunch.
- **Smart default:** if the v1 default template is still "none" (-1), v2 pre-selects *Price inquiry reply* as the persisted default (Settings §2B).

### 2. UI files

- **FirstRun** — "Try Demo" button: pre-fills +92 300 1234567 + Price Inquiry on Home and launches WhatsApp via the existing `openChat` path; no WhatsApp → inline "not installed" message. The demo records **no** Recent (it is not a customer) and does not dismiss first run.
- **Home** — order is now number → app chooser → **template chips** → label → CTA (label moved down). One-time tip banner ("Paste a number → Pick a template → Open Chat."), dismiss persists `homeTooltipShown`, never shown again. Chips sort the preferred language first (stable sort); selection initialises from the persisted default.
- **Recents → action hub** — Pulse row on top (see interpretation log for the Paid rule); status chip per row cycling INQUIRY → ORDERED → PAID → SHIPPED, cycling **into** PAID opens the amount prompt (currency-symbol prefix, Skip allowed → amount stays null); waiting clock for INQUIRY rows ("Waiting 25m" / "Waiting 3h 20m" / "Waiting 2d 4h"; orange < 24 h, red ≥ 24 h) driven by a 60-second `LaunchedEffect` tick shared with the Pulse week window; memo pinned under the name; "Reminder set" / "Reminder set (in-app)" line while a follow-up is in the future. Row tap → bottom sheet (Open Chat / Follow-up in 2 days / Add Memo / Mark Paid). v1's long-press menu, swipe-delete, search and clear-all unchanged (clear/delete now also cancel any scheduled follow-up work).
- **Templates** — filter tabs gain Hindi / Tamil / Telugu / Marathi (row scrolls horizontally); purpose shown as a small tag on each row.
- **Urdu typography** — `ui/components/TemplateText.kt`: Urdu-script template text (bodies, previews, Home chips, the editor field) renders in the subsetted Nastaliq with an explicit RTL layout direction and 30 sp line height (Nastaliq stacks clip without it). Numbers inside stay LTR via Unicode bidi — nothing is force-mirrored. All other scripts use system fonts.
- **Settings** — Default Template picker no longer offers "No template" (per-send "No template" on Home is untouched); new Preferred Template Language (Any + 6) and Default Status (default Inquiry) dropdowns; the "Your data stays on your phone" block unchanged and prominent.

### 3. Manifest + resources

- `AndroidManifest.xml` declares **exactly two** permissions (audit below): `POST_NOTIFICATIONS` + `com.google.android.gms.permission.AD_ID` (the AD_ID line written explicitly so the merged manifest shows it on purpose; AdMob uses it for ad delivery only). The header comment was rewritten to describe both. `MainActivity` is now `singleTop` so a follow-up notification tap re-enters cleanly through `onNewIntent`.
- `res/font/noto_nastaliq_urdu.ttf` — Noto Nastaliq Urdu, **subsetted** (method below), 899,288 bytes.
- Gradle: `work-runtime-ktx` 2.9.1 added (version catalog + app module). **versionCode 4 / versionName "2.0"** — see the interpretation log: v1 shipped versionCodes 1–3, so 2 was impossible.

### Interpretation log (every judgement call, in one place)

- **POST_NOTIFICATIONS, sanctioned exception implemented as specified:** declared in the manifest; requested **only** from the Recents "Follow-up in 2 days" action (`rememberLauncherForActivityResult`, API 33+); denial still saves the reminder with `followUpNotified = 0` and the row reads "Reminder set (in-app)". Scheduling is WorkManager unique work per number (`followup_<number>`, REPLACE); the notification's PendingIntent carries the number + app package and `MainActivity` deep-links into that chat. Deleting/clearing recents cancels the work.
- **Tamil / Telugu / Marathi seed templates are AI-DRAFTED, pending native review.** The 3 Hinglish bodies are byte-verbatim from the spec; the 9 Tamil/Telugu/Marathi bodies were written for this build (colloquial, same `{name}` / `{item}` / `{price}` / `[payment method]` tokens) and must be checked by native speakers before being treated as final copy. Their titles follow the v1 convention (English title + language suffix).
- **Pulse "Paid" counts status == PAID only** — SHIPPED is not folded in. "This Week" sums `paidAmountMinor` where `paidAt` is within the last 7 days; no payments → the row shows the currency symbol + 0 (a real zero, never a placeholder).
- **Purpose is a tag, not sections** — shown as a small tag on template rows under the existing language filter (the spec's simplest faithful option).
- **Amounts are stored in WHOLE units** in `paidAmountMinor` (the spec's column name kept): 12500 means Rs 12,500. Simplest exact option — no paisa/cent fractions anywhere in the prompt or the Pulse sum.
- **versionCode is 4, not 2.** The task brief said "versionCode 2", but the v1 codebase on disk is already at versionCode 3 (v1.0.2, the third CI build). Android refuses to install a lower-or-equal code over it, which would break the install-over-v1 acceptance check. versionName is "2.0" exactly as specified.
- **Recents search also matches the template title** *(founder micro-fix from the v1.0 phone test)*: `RecentDao.observeFiltered` now ORs `templateTitle LIKE …` alongside label/number — searching "order" finds the entry that used "Order confirm".
- **Template-editor placeholder row scrolls horizontally** *(founder micro-fix)*: the {name} {price} {item} {qty} row clipped {qty} on the founder's phone; it is now a horizontally scrollable row, so all four buttons are always fully visible and tappable.
- **Try Demo records no Recent and keeps first-run open** — it is a preview of the trick, not a customer interaction.
- **Font subset decision:** instanced the variable font to static weight 400, then `fonttools subset` with `--no-hinting`, unicodes = basic Latin (U+0020–007E) + U+00A0 + U+0600–06FF + U+0750–077F, default layout features — **899,288 bytes** (vs 1,033,544 unsubsetted static; the full variable font is 690,304 bytes but ships every weight 400–700, which the app never uses). Dropping the FB50–FDFF / FE80–FEFF presentation-form cmap ranges saved ~9 KB (those glyphs are still reached by shaping, not typed directly). All shaping features retained (ccmp, curs, fina, init, isol, kern, mark, medi, mkmk, rlig — verified) and every codepoint in the Urdu seed strings verified present. AAPT deflates the TTF inside the APK, so the on-disk budget cost is lower than the raw figure; CI's printed APK size is the arbiter.

### v2 acceptance additions (verified at CI / on phone — see UPDATE_INSTRUCTIONS.md)

| # | Criterion | How |
|---|---|---|
| 12 | v1 templates / recents / labels survive the update | ALTER-only migration; install OVER v1.0, do not uninstall first |
| 13 | Permission audit: exactly POST_NOTIFICATIONS + AD_ID added vs v1 | `grep uses-permission app/src/main/AndroidManifest.xml` → the two lines, nothing else |
| 14 | No permission ask at launch or first run | The only `RequestPermission` launcher lives in RecentsScreen's follow-up action |
| 15 | Try Demo / waiting clock / chip cycle / memo / Pulse row | Phone checks listed in UPDATE_INSTRUCTIONS.md |

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
2. Dependency diet — see the table in README.md. The two big calls: material-icons-**core** (not extended; missing glyphs are 2 local vectors) and ZXing **core** (not the embedded/camera artifact). No Firebase, no Analytics, no image loader, no networking stack, vector drawables only. *(v1 note: "no custom fonts — system font renders Urdu fine" — superseded in v2 by the one subsetted Nastaliq font for Urdu script, justified in the README table and the v2 section above.)*
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
| 2 | Permissions: v1 declared zero; **v2 declares exactly two** (POST_NOTIFICATIONS — asked in-context only, on the follow-up tap; AD_ID — AdMob, declared explicitly) | Manifest + the single `RequestPermission` launcher in RecentsScreen | `grep uses-permission app/src/main/AndroidManifest.xml` → exactly the two v2 lines |
| 3 | Open → WhatsApp chat in < 5 s | One intent, no intermediate screens; the Recents write happens asynchronously *after* launch | Phone test with a stopwatch |
| 4 | Template text lands in the message box, never auto-sent | wa.me `?text=` pre-fill only; no send automation exists | Phone test: text present, unsent |
| 5 | WhatsApp **and** WhatsApp Business paths work | Package-aimed intents + `<queries>` detection + failure dialog offering the other app | Phone test on a dual-app phone |
| 6 | Never shows a "number not on WhatsApp" pre-check | No such check is implemented, anywhere | Code grep + phone test with an unregistered number |
| 7 | No interstitial in the core flow | Only an `AdView` banner composable exists; no InterstitialAd/AppOpenAd/RewardedAd imports | Code grep |
| 8 | Recents/templates/labels survive restart + update | Room database file + SharedPreferences; no in-memory-only state | Phone test: kill app, relaunch, update install |
| 9 | Urdu + Roman Urdu templates display correctly | Urdu bodies render inside an explicit RTL layout-direction provider in lists, previews and the editor — **v2: in the bundled subsetted Nastaliq font** | Screenshot check |
| 10 | Shared text with a number pre-fills Home | `ACTION_SEND` intent-filter + digit-run extraction in `PhoneUtils.extractNumber` | Phone test: Share a number from any app → SellerChat |
| 11 | Works in airplane mode (except the ad) | Every feature is local; the only network user in the app is the AdMob banner | Phone test with airplane mode on |
