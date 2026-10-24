# SellerChat

**Message any customer on WhatsApp or WhatsApp Business — with your price and order reply already typed.**

One screen: paste a number → pick WhatsApp / WhatsApp Business → the chat opens with your chosen template already in the message box. Plus labeled recents, reusable templates in English / اردو / Roman Urdu / Hindi / Tamil / Telugu / Marathi / French / Spanish / Arabic / Chinese, a wa.me link + QR generator, and a one-tap "share my link to my status".

Built for small online sellers in Pakistan, India, Bangladesh, the UAE and Nigeria — and WhatsApp sellers everywhere. **No account, no cloud, no backend. All data stays on the phone.** Nothing is asked at launch; the only permission prompt in the app appears if *you* tap "Follow-up in 2 days" on a customer (notifications, Android 13+ — and the reminder still saves in-app if you decline). One AdMob banner on the Home screen only — never in the chat flow.

Package: `com.fahad1005.sellerchat` · Min SDK 24 · Target SDK 35 · Kotlin + Jetpack Compose (Material 3)

## What's new in v2.6

- **Full khata ledger.** Per-customer books inside the app: Udhaar (credit given), Payment (cash / JazzCash / Easypaisa / bank), Advance (customer paid extra). Balances always read in words — "To receive Rs 600", "Advance Rs 200", "Settled" — partial payments just reduce the balance, and a clean text statement shares via WhatsApp. Udhaar entries can carry a due date with a reminder that opens WhatsApp pre-filled on the day (nothing sends itself; a settled debt never nags).
- **Multi-business.** Run several shops in one app — switch from the Money tab header. Money books (khata, invoices, products, expenses) are per shop; customers, templates and scheduled messages are shared. Your existing data opens under "My Business" with zero action. Manage shops in Settings: add (name + currency), rename, delete — delete is refused while a shop still holds records.
- **Money tab + Today card.** Three tabs only: Sell, Customers, Money. Money shows Today (sales, owed, expenses, profit for the selected shop) plus the khata, expenses, business-tool links and Share-my-shop. Sell's top card is the same business in one line of figures, each tapping through to its tab.
- **Auto follow-up suggestions.** "Follow up today" on Customers lists waiting customers (2+ days) and overdue khata payments automatically — no per-customer setup. One tap opens WhatsApp pre-filled.
- **Multichannel open-chat (links only).** "Send on another app" on Sell opens Telegram, Messenger or SMS for the customer. Telegram/Messenger open the chat but do not pre-fill the message (the app says so); SMS opens with the text ready. If the target app can't open, WhatsApp opens instead, with a note. SellerChat does NOT receive messages from any channel — a true inbox needs a backend + per-seller API setup and is out of scope.
- **Inline discount calculator.** Pick a product, type a % — the final price computes live and fills {price} in the template.
- **Share my shop + ad link.** One shareable text: your shop's products with prices plus a wa.me link with a pre-filled "I saw your ad" message and an optional source tag, so you learn which ad brought the customer.
- **Delivery stages.** The status chip now cycles Inquiry → Ordered → Packed → Shipped → Delivered → Paid.
- **Also carried forward:** everything from v2.5, the v2.4 strict phone validation, the v2.4.1 Chinese-template parser fix, the real privacy-policy URL, and the Play API-36 toolchain.

## What's new in v2.5

- **Invoices.** A bill book: create invoices (INV-001, INV-002, … — numbers are never reused) for an existing customer or a typed number, with lines, an auto total, and an Unpaid/Paid filter. "Send via WhatsApp" opens the chat with the invoice text pre-filled; "Mark Paid" marks the invoice paid AND marks the linked customer Paid with the invoice total, so Pulse picks the payment up.
- **Product catalog.** Your own price list with full add/edit/delete and an active toggle. Pick a product on Home and it fills {item} and {price} in the template preview through the same shared fill function — preview and send always match. "Send product" opens WhatsApp with the product message.
- **Send photo/file to unsaved numbers.** New "Send photo/file" actions on Home, on the Recents sheet, and on catalog rows: pick a file, WhatsApp opens with it attached to that number's chat and the caption typed. Honest copy throughout: WhatsApp shows the file attached and you tap send there — nothing is ever claimed as sent from inside SellerChat. If attaching fails, the chat still opens with the caption and the app tells you to attach the file in WhatsApp.
- **Scheduled messages (reminder-to-send, never auto-send).** Pick a customer, a template (filled at creation, so you approve exactly what will open) or custom text, and a date/time. At that time a notification fires; tapping it — or "Send now" in the list — opens WhatsApp with the text pre-filled and marks the item Sent. Dismissing the reminder leaves it due in the list. The notification permission is asked in-context at the schedule tap, exactly like follow-ups; denied still saves the item in-app.
- **Contact import + clipboard auto-detect (zero new permissions).** On returning to Home, a number in the clipboard is offered as a dismissible "Use it" chip — the clipboard text is never stored. "Import from contacts" uses the system picker, which grants access to the picked contact only, and fills the number and label.
- **Assisted Greeting / Away (NOT auto-reply).** Settings gains business hours plus editable Greeting and Away texts (and the Templates library gains editable built-in "Greeting" and "Away" templates). When away mode is on and the hour is outside business hours, Home shows an Away banner with a one-tap "Send away message" button; for a first contact, Home suggests the greeting for one send. Nothing watches notifications, nothing sends in the background — true hands-free auto-reply needs the WhatsApp Business API and is out of scope (see BUILD_NOTES).
- **Also carried forward:** the Play API-36 toolchain (compileSdk/targetSdk 36, AGP 8.9.1, Gradle 8.11.1) and the v2.4.1 Chinese-template parser fix (CJK sentence delimiters 。！？, Arabic ؟, and the 元 currency-orphan marker) — both verified present in this build.

## What's new in v2.3

- **Global Smart Localization.** On the first v2.3 launch — and only once — SellerChat detects your country (SIM first, then device language/region), your currency, and your preferred template language, and sets all three up for you. After that, **Settings → Region & Currency** is yours: country, currency (PKR, INR, BDT, USD, GBP, EUR, AED, SAR, NGN) and template language are manual choices that override the detection forever. The chosen currency drives the Mark Paid prompt, the weekly Pulse total, and the message cleanup rules. Detection uses only permission-free system APIs — no location, no prompt, no network.
- **12 new templates: French, Spanish, Arabic and Chinese** (price, payment, delivery in each), written in each market's own style — postposed € in French/Spanish, generic amount wording in Arabic, {price} 元 in Chinese. Arabic templates render right-to-left in the system font; the bundled Nastaliq stays Urdu-only and no new fonts ship. ⚠️ These 12 bodies are **AI drafts pending native review**, like the Tamil / Telugu / Marathi sets before them.
- 36 seed templates in total, and the v2.2 message guarantees carry forward untouched: no raw brackets or placeholders ever reach a customer, and an empty price never strands a currency symbol.

## What's new in v2.2

- **Forward to Teammate.** Save up to 3 teammates in Settings (name + number, on-device only), then hand any customer over from the Recents sheet: WhatsApp opens on the teammate's number with "Customer: X. Please take over this chat." already typed. Forwarding never creates a Recent — teammates are staff, not customers.
- **Cleaner messages, guaranteed.** The shared fill now also removes unfilled `[…]` hints and multi-word `{…}` tokens, drops a currency marker stranded by an empty price ("Rs", "₹", "روپے"), and swaps the order-confirmation total clause for a complete sentence in English, Roman Urdu and Urdu — a customer never sees a raw bracket or a broken "Total Rs ." again.
- **Seed wording fixed.** The Roman Urdu (and Urdu) order confirmations no longer double their "ka" when the item is blank, and the payment templates use a `{payment method}` token instead of bracketed text. A one-time correction pass rewrites stored defaults only where they still match the old wording byte-for-byte — your edits are never touched.
- Same fixed signing key as v2.1: v2.2 installs straight over it, data intact — it doubles as the install-over proof build.

## What's new in v2.1

- **Permanent signing.** Builds now sign with your own fixed key (kept in repo secrets, set up once — see UPDATE_INSTRUCTIONS.md), so updates install on top of each other and data survives. Every CI run page prints `SIGNING: FIXED KEY` or `SIGNING: THROWAWAY FALLBACK (secrets not set)` — no guessing which mode a build used.
- **See it before you send it.** Tapping a template chip shows the filled message right under the chips — the label is parsed as "Name — item", so `Ayesha — kurta` fills `{name}` and `{item}` automatically, and leftover `{…}` tokens are stripped: a customer never sees a raw bracket. The preview and the send use one shared fill function, so they always match.
- **Keyboard-safe Home.** The content scrolls, the bottom navigation hides while you type, and the banner is pinned back at the bottom of Home (v2's layout could push it off-screen).
- **Calmer Recents cards.** Status is now a small colored dot + outline badge next to the customer's name (orange / blue / green / purple); the template used is plain text under the number.
- **Smarter chip order.** With no preferred language set, chips follow your country (Pakistan → English, Urdu, Roman Urdu first; India → Hindi, English); "No template" moved to the end of the row.
- CI now also builds the release **AAB** and reports APK + AAB sizes on every run.

## What's new in v2.0

- **Try Demo** on the first-run screen: one tap opens WhatsApp with a sample number + the price reply, before any setup.
- **Recents is now a mini-CRM.** Every customer gets a status chip (Inquiry → Ordered → Paid → Shipped, tap to cycle), a waiting clock ("Waiting 3h 20m", orange under a day, red after), a pinned memo, and a row-tap action sheet: Open Chat / Follow-up in 2 days / Add Memo / Mark Paid. Marking Paid optionally records the amount.
- **Pulse dashboard** at the top of Recents: `Leads: N | Waiting: N | Paid: N | This Week: Rs 12,500` — with your currency symbol and Indian digit grouping when your country is India.
- **Follow-up reminders** via WorkManager + a notification that deep-links straight into that customer's chat. The notification permission is asked in-context only, never at launch.
- **12 new templates**: Hinglish (Hindi) price/payment/delivery, plus Tamil, Telugu and Marathi versions — 24 seed templates in 7 languages. Urdu-script templates now render in a bundled, subsetted **Noto Nastaliq Urdu** font with proper RTL.
- **Smarter Home:** template chips moved above the label field, a one-time tip banner, a Preferred Template Language setting (your language's chips come first), and a Default Template that can no longer be "No template".
- **v1 data survives:** the Room 1→2 migration is ALTER-only — templates, recents and labels carry over untouched.

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

**Signing (v2.1 model):** the app has ONE permanent key, alias `sellerchat`, in a PKCS12 file (`sellerchat.p12`) generated once locally by the lead agent. The founder received it as a base64 text file and pasted it into repo secrets (`SIGNING_KEYSTORE_B64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`) — the .p12 and its base64 live outside this repo and are never committed — full founder steps in UPDATE_INSTRUCTIONS.md. CI decodes it to a temp file on the runner (never printed, never committed) and `app/build.gradle.kts` signs release with it via the `permanent` signing config (env `SELLERCHAT_SIGNING_*`, storeType PKCS12). If the secrets are absent, CI falls back to a *throwaway* runner keystore so builds never hard-fail — but fallback builds cannot update fixed-key installs, and the run page says so (`SIGNING: THROWAWAY FALLBACK (secrets not set)` vs `SIGNING: FIXED KEY`). Locally, with no env vars set, the release build is unsigned. When you publish, this key becomes your Play **upload** key under Play App Signing. Guard the passwords: losing them means losing the ability to update the app under this identity.

No Gradle wrapper is committed (the build machines here have no Java to generate one); CI installs Gradle 8.9 via `gradle/actions/setup-gradle`. With Android Studio, just open the project root and it will offer to create the wrapper / sync as usual.

## Swap the AdMob IDs — TODO(FAHAD)

The app currently uses Google's public **sample** AdMob IDs (test ads only, zero revenue). Replace both before any real release:

1. **App ID** — in `app/src/main/AndroidManifest.xml`, the meta-data `com.google.android.gms.ads.APPLICATION_ID` (currently `ca-app-pub-3940256099942544~3347511713`).
2. **Banner unit ID** — in `app/src/main/java/com/fahad1005/sellerchat/ui/components/AdBanner.kt`, `SAMPLE_BANNER_AD_UNIT_ID` (currently `ca-app-pub-3940256099942544/6300978111`).

There is exactly one ad: a banner pinned to the bottom of the Home screen. There are no interstitial, app-open, or rewarded ads anywhere, by design.

## Edit the seed templates

The 12 first-launch templates live in one file:

`app/src/main/java/com/fahad1005/sellerchat/data/SeedData.kt`

Edit the `title` / `body` / `language` of any entry, or add new ones (keep `sortOrder` increasing). Placeholders are substituted when a chat is opened (and in the Home preview — one shared fill function, `MainViewModel.filledTemplateText`): the customer label is parsed as "Name — item", so `{name}` gets the name and `{item}` gets the item; `{price}` / `{qty}` are blanked, and any leftover `{…}` token is stripped, so a raw placeholder never reaches a customer. Note: seeds are inserted only when the templates table is empty (first install); changing seeds does not rewrite templates on phones that already ran the app — users can edit/delete every seeded template in the app itself.

The 12 v2 templates live in the same file (`v2Templates()`): they are inserted exactly once per install — fresh installs *and* v1 upgraders — behind the persisted `seedsV2Done` flag, appended after the existing library. The Tamil / Telugu / Marathi bodies are AI drafts pending native-speaker review (see BUILD_NOTES.md); the 3 Hinglish bodies are verbatim from the product spec.

The 12 v2.3 templates live in the same file (`v4Templates()`): French, Spanish, Arabic and Chinese × 3, inserted exactly once behind the persisted `seedsV4Done` flag, which runs **after** the v2 insert and the v2.2 seed corrections (`seedsV3Done`) — an upgrader from v2.0 or v2.1 runs all three passes in order on their first v2.3 launch, and a title+language guard makes duplicates impossible. **All 12 v2.3 bodies are AI drafts pending native-speaker review.**

## Verify the size budget (release < 5 MB)

The release budget is a hard product constraint. CI prints a size report on every build (Actions → the run → "Report APK sizes"). To verify locally:

```bash
gradle :app:assembleRelease :app:bundleRelease
ls -la app/build/outputs/apk/release/app-release.apk   # must stay under 5,000,000 bytes
ls -la app/build/outputs/bundle/release/app-release.aab # same budget — CI reports both on every run
```

How the budget is kept — every dependency justified:

| Dependency | Why it is allowed in |
|---|---|
| Jetpack Compose + Material 3 (BOM 2024.12.01) | The UI itself. R8 full mode + resource shrinking strip everything unused. |
| material-icons-**core** only | The *-extended* icon pack is megabytes of vectors. Missing glyphs (QR, chat bubble) ship as two tiny local vector drawables instead. |
| Room (runtime + ktx + compiler) | Templates + recents need ordered, searchable, durable local storage; Room is the smallest *safe* way to do that in Kotlin. |
| WorkManager (work-runtime-ktx 2.9.1) — **v2** | Follow-up reminders that survive reboot and Doze without an exact-alarm permission. The sanctioned scheduling cost (~200–400 KB after R8); the only new dependency in v2. |
| Subsetted Noto Nastaliq Urdu font — **v2** | Urdu-script templates render in real Nastaliq instead of the system fallback. Subsetted with fonttools to the Arabic blocks + Latin/digits, static weight 400, no hinting: 899,288 bytes raw in `res/font/` (deflate-compressed inside the APK), vs 1,033,544 bytes for the unsubsetted static font. Hindi/Marathi/Tamil/Telugu use system fonts — no other font is bundled. **v2.1 audit:** a tighter re-subset (no Latin letters) measured only 11,968 bytes smaller and dropped letters used by the placeholder tokens inside the Urdu bodies — rejected; the 899,288-byte cut stays (see BUILD_NOTES). |
| play-services-ads 23.6.0 | The single Home banner. The largest single line item; accepted because ads are the v1 business model. No other Google services (no Firebase, no Analytics) are included. |
| ZXing **core** 3.5.3 | QR *generation* only (~500 KB pre-shrink, much less after R8). The android-embedded/camera variants were rejected — no scanning is needed. |
| core-ktx, activity-compose, lifecycle | Small AndroidX glue required by Compose/ViewModel. |
| Nothing else | No DI framework, no navigation library (in-app state routing), no image loaders, no networking stack, no DataStore (SharedPreferences covers settings at zero dependency cost). |

Release config doing the work: `isMinifyEnabled = true` (R8 full mode via `android.enableR8.fullMode=true`), `isShrinkResources = true`, vector drawables only, one subsetted font (Urdu script only).

## Project layout

```
app/src/main/java/com/fahad1005/sellerchat/
├── MainActivity.kt          — entry point, ACTION_SEND pre-fill
├── SellerChatApp.kt         — Application, AdMob init
├── data/                    — Room entities/DAOs/database (+ the 1→2
│                              migration), seed templates, country list,
│                              SharedPreferences settings
├── notify/                  — follow-up reminders (WorkManager, v2)
├── util/                    — phone normalization, QR generation, time,
│                              currency formatting
├── viewmodel/               — MainViewModel (MVVM, no DI framework)
└── ui/                      — Compose screens in spec order:
                               Home, Recents, Templates, QR, ShareStatus,
                               Settings, FirstRun + theme/components
```

Privacy: see `PRIVACY_POLICY.md`. Store listing draft: see `STORE_LISTING.md`. Build decisions: see `BUILD_NOTES.md`.
