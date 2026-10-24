# SellerChat — Build Notes

## v2.6 (winner pack — built on v2.5; SUPERSEDES the uninstalled v2.5)

**versionCode 11 / versionName "2.6".** The "one app runs the whole business" build: full khata ledger, multi-business, a Money tab, a Today card, auto follow-ups, multichannel open-chat, an inline discount calculator, a shareable shop card, and delivery stages — all on-device, no backend, no account. **No new Gradle dependency, no new runtime permission** (manifest untouched — still exactly POST_NOTIFICATIONS + AD_ID), **no new assets or fonts, code only** (release-size impact is a few dozen KB of Kotlin + two new screens). Every existing feature and every existing row of user data is preserved; the Room change is one migration, 3 → 4 (below).

### Baseline carried forward (do not regress)
Everything from v2.5 (invoices, catalog, media send, scheduled messages, contact import/clipboard, assisted greeting/away), the v2.4 strict per-country phone validation, the v2.4.1 Chinese-template parser fix (`splitSentences` splits on `。` `！` `？` and `؟`; orphan markers include `元`), the real privacy URL (`https://fahad1005.github.io/SellerChat/privacy-policy.html`), and the Play API-36 toolchain (compileSdk/targetSdk 36, AGP 8.9.1, Gradle 8.11.1; the workflow is unchanged since v2.4.1 and is NOT in the v2.6 zip).

### 0 — Multi-business (the scoping rule — read this first)
One app, several shops (e.g. a clothing shop and a food business), switched from a dropdown in the Money tab header. **Scoping rule: money objects are per business; people are global.** Khata entries, invoices, invoice lines, products and expenses carry a `businessId`; Recents (customers), templates and scheduled messages stay GLOBAL — one person can buy from both shops, and a reminder is about a person, not a shop. The Money tab's figures (Today card, khata, invoices, products, expenses) all follow the selected shop, in that shop's currency; the Home Today card labels its money figures with the shop name ("Today · My Business") and labels waiting/follow-up counts "All shops". The selected shop persists in `SettingsStore.selectedBusinessId`. "Manage businesses" lives in existing Settings (no new screen): add (name + currency from the 9 supported codes), rename, delete — delete is refused while the shop holds ANY khata/invoice/product/expense rows, with an honest note saying why. If the deleted shop was selected, selection falls back to "default". The default business ("My Business", id `default`) is seeded by the migration and adopts the seller's Settings currency on first v2.6 launch (see Migration note below) — a v2.5 user opens v2.6 and finds everything under "My Business" with zero action.

### 1 — Full khata ledger (`KhataEntities.kt`, `MoneyScreen.kt`)
Per-customer ledger keyed by phone number, inside the selected shop. Entry types: **Udhaar** (credit given — customer owes more), **Payment** (money received — cash / JazzCash / Easypaisa / bank recorded), **Advance** (customer paid extra — seller owes goods). Each entry: whole-unit Long amount (same convention as invoices/Pulse), date, optional note; udhaar rows can carry a due date. **Balance rule (one rule, everywhere): udhaar − payments − advances.** Positive = "To receive Rs X", negative = "Advance Rs X", zero = "Settled" — the sign is always in words, never left to a minus sign. Partial payments are just more PAYMENT rows; nothing is ever overwritten. List filters All / To receive / Advance / Settled + search by name/number. Due-date reminders reuse the existing WorkManager/notification pattern on the existing follow-ups channel (no new channel, no new permission): at the due date a notification fires, tapping it builds the reminder text from the LIVE ledger and opens WhatsApp pre-filled — the seller taps send; a debt settled after scheduling sends nothing (the deep link returns null). Deleting an entry cancels its reminder. Share statement generates a clean text statement (entries oldest-first + balance line) through `sanitizeOutgoing`, shared via ACTION_SEND. A customer with no entries yet is reached from the Money tab's "Add entry for a customer" picker (recent customers + typed name/number).

### 2 — Money tab + Today card (the clean-UI design — 3 tabs, never 4)
Bottom tabs are now **Sell / Customers / Money** — Templates left the tab bar and is a pushed screen from Home's Business-tools card (with a Back button, like every pushed screen). Money tab, top to bottom: shop switcher → Today card (sales today, owed to you, expenses today, profit = sales − expenses, all for the selected shop in its currency; waiting/follow-up counts labelled "All shops") → Khata → Expenses (add → amount → save, feeding the profit line) → links to the existing Invoices / Catalog / Scheduled screens (linked, not duplicated) → "Share my shop". Home's Today card is the same idea in one line of figures, each figure tapping through to its tab. **"Today sales" definition (documented, not hidden): khata payments received today for this shop.** Invoice Mark Paid still updates the customer's global status (v2.5 behavior) — the receipt itself belongs in the khata for the shop's books. Design laws enforced: nothing deeper than 2 taps from a tab; one primary button per screen; numbers before words; no new settings pages (only on/off-style additions inside existing Settings); every form is the same "add → amount → save" shape.

### 3 — Auto follow-up suggestions (Customers tab)
No manual per-customer setup: "Follow up today" lists (a) Recents in INQUIRY whose waiting clock is older than 2 days, and (b) khata customers past their due date with a positive balance. One tap opens WhatsApp pre-filled (waiting check-in text / live-balance reminder text) and records the Recent. The manual "Follow-up in 2 days" reminders are untouched.

### 4 — Multichannel open-chat (LINKS ONLY — read the boundary)
On Home, "Send on another app" opens a channel picker: **Telegram** (t.me/&lt;username&gt;), **Messenger** (m.me/&lt;username&gt;), **SMS** (ACTION_SENDTO with the text pre-filled). Usernames are NOT phone numbers, so they live in a new `channel_handles` table keyed by number (never on the Recent row); the picker asks inline once and offers to save. Telegram/Messenger links do NOT carry seller text — the UI says plainly that the message is not pre-filled there. If the target app cannot open, the wa.me chat opens instead, with an in-app note saying so. UI copy never claims a message was sent from inside SellerChat and never claims replies arrive in SellerChat. **Deliberately NOT built: any inbox, notification reading, automation or auto-send.** A true multichannel inbox needs a backend + per-seller API setup per channel (Telegram Bot API, Meta review for Messenger/Instagram, WhatsApp Business API) and is a Pro-tier SaaS product, not a free utility.

### 5 — Discount calculator (inline, Sell screen)
With a product picked, an optional % field computes the final price live ("Rs 500 − 10% = Rs 450") and the VM's shared fill uses the discounted whole-unit price for {price} — preview and send still always match. No new screen; the scheduled path fills with no product and is never discounted.

### 6 — Catalog card + ad link ("Share my shop", Money tab only)
One shareable text: the selected shop's active products with prices + a wa.me click-to-chat link for the seller's own number with a pre-filled "I saw your ad" message and an optional source tag ("on Instagram"), so the seller learns which ad brought the customer. No tracking, no shortener, no backend. The seller's number field is prefilled from the saved "my number" (Share-to-Status) and saved back on share.

### 7 — Delivery stages (status pipeline extended, not replaced)
`RecentStatus` gains PACKED and DELIVERED; the chip now cycles INQUIRY → ORDERED → PACKED → SHIPPED → DELIVERED → PAID → INQUIRY (PAID keeps its amount prompt). Old rows stored INQUIRY / ORDERED / PAID / SHIPPED by name — enum values are only ever added, never renamed, so they keep working.

### Migration: Room 3 → 4 (`MIGRATION_3_4`, single migration)
CREATE-only new tables: `businesses`, `khata_entries`, `channel_handles` (+ its unique index), `expenses`. **One relaxation of the old "never alter a table" rule, and only this one:** `invoices`, `invoice_lines` and `products` each gain `businessId TEXT NOT NULL DEFAULT 'default'` via ALTER TABLE ADD COLUMN — ADD COLUMN with a default backfills every existing row without touching any other column, so v2.5 data lands under "My Business" untouched. The entity side declares `@ColumnInfo(defaultValue = "default")` on every `businessId` column, matching the SQL defaults exactly (the v2.0 pattern — Room validates defaults on first open). The `businesses` seed row is `('default', 'My Business', 'USD', 0)`; `createdAt = 0` is the "adopt me" marker because a migration cannot read SharedPreferences — first v2.6 launch (`ensureDefaultBusiness`) rewrites it with the seller's Settings currency, and fresh installs (which never run migrations) insert the default business with the Settings currency directly. No destructive fallback is registered anywhere.

### Deliberately NOT built (and why)
- **In-app inbox / auto-reply** (any channel): needs backend + per-seller API onboarding; notification/accessibility automation risks number bans + Play removal. The greeting/away boundary from v2.5 stands.
- **Interest calculation, multi-currency-per-customer, cloud sync/backup export:** out of scope for a light ledger; backup/export was pitched as a winner feature but is NOT in v2.6 (it needs a file + restore flow of its own — next build, not smuggled in here).
- **Bulk broadcast:** one-by-one opens only; bulk messaging to customer lists is a ban risk and stays out.
- **Full accounting (categories, receipts, tax):** the expense log is deliberately a light log, not accounting.

### v2.6 file list (repo-relative)
New: `app/src/main/java/com/fahad1005/sellerchat/data/KhataEntities.kt`, `app/src/main/java/com/fahad1005/sellerchat/notify/KhataReminders.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/MoneyScreen.kt`. Changed: `app/build.gradle.kts` (11 / 2.6), `app/src/main/java/com/fahad1005/sellerchat/data/Entities.kt` (PACKED, DELIVERED), `app/src/main/java/com/fahad1005/sellerchat/data/BusinessEntities.kt` (businessId), `app/src/main/java/com/fahad1005/sellerchat/data/Daos.kt` (BusinessDao + scoped queries + counts), `app/src/main/java/com/fahad1005/sellerchat/data/AppDatabase.kt` (version 4), `app/src/main/java/com/fahad1005/sellerchat/data/Migrations.kt` (3 → 4), `app/src/main/java/com/fahad1005/sellerchat/data/SettingsStore.kt` (selectedBusinessId), `app/src/main/java/com/fahad1005/sellerchat/viewmodel/MainViewModel.kt` (business selection/scoping, khata, expenses, channels, discount, catalog card), `app/src/main/java/com/fahad1005/sellerchat/MainActivity.kt` (khata deep link), `app/src/main/java/com/fahad1005/sellerchat/ui/MainScreen.kt` (Sell/Customers/Money tabs), `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt` (Today card, discount, channel picker, Templates link), `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt` (stages, Follow-up-today), `app/src/main/java/com/fahad1005/sellerchat/ui/SettingsScreen.kt` (Manage businesses), `app/src/main/java/com/fahad1005/sellerchat/ui/TemplatesScreen.kt` (Back button), plus `README.md`, `BUILD_NOTES.md`, `UPDATE_INSTRUCTIONS.md`. Manifest, `libs.versions.toml` and the workflow are UNCHANGED.

### Risks (CI is the proof — nothing here was compiled)
- Room 3 → 4 schema validation on first open of a v3 database — the ALTER COLUMN defaults + the unique index + the new tables must match the entities exactly (see Migration note). If CI's install-over test opens a v3 database cleanly, this risk retires.
- The new Money/Khata dialog compositions (khata entry dialog, customer picker, business switcher dropdown) — untested layouts.
- The t.me / m.me fallback paths (target app missing, username with @, Telegram not installed) — the wa.me fallback + note covers failures, but the exact behavior per phone needs the phone test.
- Discount + template fill interaction (discounted price in {price} sentences, empty-price rules skipped) — needs one phone check with a product + discount + a {price} template.

## v2.5 (business tools — built on v2.4.1)

**versionCode 10 / versionName "2.5".** Six features, all on-device, no backend, no account. **No new Gradle dependency, no new runtime permission** (manifest untouched — still exactly the two v2 lines), **no new assets or fonts, code only** (release-size impact is a few dozen KB of Kotlin + three small screens). Every existing feature and every existing row of user data is preserved; the Room change is CREATE-only (below).

### Baseline carried forward (do not regress)
The local tree was at 2.4.1 but still on SDK 35. The shipped v2.4.1 zip's toolchain is carried into this build verbatim: `compileSdk = 36`, `targetSdk = 36`, AGP 8.7.3 → 8.9.1 (`gradle/libs.versions.toml`), workflow installs `platforms;android-36` + `build-tools;36.0.0` and Gradle 8.11.1 (`.github/workflows/build.yml`). The v2.4.1 Chinese-template parser fix is also carried forward — and was missing from the local tree, so it is re-applied here, not assumed: `splitSentences` now splits on `。` `！` `？` and `؟` in addition to `.` `!` `?` `۔`, and the currency-orphan marker list gains `元`. The privacy-policy URL (`SettingsScreen.kt`) was verified already correct in the tree and is untouched.

### 1 — Invoices (`BusinessEntities.kt`, `InvoicesScreen.kt`)
`Invoice` (display number INV-001… from max row id + 1, so deletes never reuse a number; optional `recentNumber` link to a Recent row) + `InvoiceLine` (description, qty, amount). Amounts are **whole currency units as Long** — the exact Mark Paid / Pulse convention (`paidAmountMinor` is whole units despite its name), so invoice totals feed Pulse sums with no conversion. List filters Unpaid/Paid; detail shows lines + total + notes; "Send via WhatsApp" uses the existing open-chat path with invoice text passed through `sanitizeOutgoing` (below); "Mark Paid" sets the invoice PAID and, when linked, the Recent PAID with `paidAmountMinor` = invoice total and `paidAt` = now, so Pulse "This Week" includes it. Invoice numbers are normalized to E.164 at creation (same strict country validation family as Home) — an invalid number shows an inline error instead of saving a broken invoice.

### 2 — Product catalog (`CatalogScreen.kt`)
`Product` (name, whole-unit price, description, active flag, sort order). Full CRUD; inactive products stay stored but leave the Home picker. Home picker chips (active products only) set `MainViewModel.selectedProduct`; the shared fill (`fillTemplate`) then uses the product name for `{item}` (only when the label gave no item) and the product price as plain digits for `{price}`. With a real price, three rules change in exactly the ways a price makes safe: the empty-price total-clause swaps are skipped ("Total: Rs {price}." becomes "Total: Rs 500."), `{price}` sentences are no longer dropped, and the currency-orphan rule is skipped (it exists only to clean up an EMPTY price slot). Without a product every rule behaves exactly as in v2.4. "Send product" opens WhatsApp with name — price — description through `sanitizeOutgoing`.

### 3 — Send media/files (`MainViewModel.sendMedia`)
System picker (`GetContent`, no permission) → `ACTION_SEND` with `setPackage` (WhatsApp / WhatsApp Business per the seller's choice), `EXTRA_STREAM` = picked content Uri, `EXTRA_TEXT` = caption, `EXTRA "jid"` = digits + `@s.whatsapp.net`, `FLAG_GRANT_READ_URI_PERMISSION`. Any failure (not installed, no activity, SecurityException) falls back to opening the wa.me chat with the caption and shows an in-app note: "WhatsApp opened without the file — attach it in WhatsApp before sending." UI copy everywhere says "WhatsApp opens with the file attached — tap send." The file is never claimed as sent from inside SellerChat. Entry points: Home ("Send photo/file"), the Recents action sheet ("Send photo/file"), catalog rows ("Send photo", with a number dialog).

### 4 — Scheduled messages (`ScheduledMessages.kt`, `ScheduledScreen.kt`)
`ScheduledMessage` (customer, app package, FINAL text, scheduled time, PENDING/SENT/CANCELLED). Template text is filled **at creation** through `filledTemplateTextFor` (same shared fill, label = customer name, no product) so the seller approves exactly what will open; custom text goes through `sanitizeOutgoing`. Scheduling is a WorkManager `OneTimeWorkRequest` keyed by row id; at fire time a notification posts on the **existing** follow-ups channel and tapping it marks the item SENT and opens WhatsApp with the text pre-filled (via a `MainActivity` deep link, extras consumed). The notification permission is asked in-context at the schedule tap, mirroring the Recents follow-up pattern; denied still saves the row, which then shows as Due in-app at its time. Cancel cancels the WorkManager job and marks CANCELLED.

### 5 — Contact import + clipboard auto-detect (zero new permissions)
Clipboard: on Home resume (foreground only), `PhoneUtils.extractNumber` runs over the clipboard text; a 7–15 digit run different from the field becomes a dismissible chip ("Number from clipboard: … — Use it"). The clipboard text is **never stored** — not in the VM, not in prefs, not in Room; only the composition-local suggestion exists, cleared on use or dismiss. Contacts: `ACTION_PICK` on `Phone.CONTENT_URI` through the system picker — reading the single picked row needs no permission (the picker grants access to that item only). The number and display name fill Home's number and label fields; empty/unreadable results show a note, never a crash.

### 6 — Assisted Greeting / Away (`SettingsStore.kt`, Settings + Home)
Settings additions: greeting on/off + greeting text, away on/off + away text, business open/close hours (0–23; open > close = overnight, e.g. 20 → 2). Behavior: (a) away on + current hour outside business hours → Home shows an "Away mode" banner with a one-tap "Send away message" button; (b) greeting on + a validated number with no prior Recent → Home suggests the greeting for exactly one send (preview shows it while active, then it clears); (c) the Templates library gains editable built-in "Greeting" and "Away" templates via the one-time, title-guarded `seedsV6Done` pass (ordinary rows afterwards — editable, deletable).

### AUTO-REPLY BOUNDARY (explicit, do not cross)
This build does **NOT** implement background auto-reply: no `NotificationListenerService`, no accessibility service, no notification reading, no background sending, no auto-send of any kind. The greeting/away features above are suggestions and one-tap sends performed by the seller in the foreground. True hands-free auto-reply (messages sent while the seller is away, without a tap) requires the **WhatsApp Business API** — a separate, Meta-approved integration with its own costs and review — and is out of scope for this app. Any future request for "reply automatically" must be routed to that API discussion, not implemented as a device-side workaround: workarounds risk the seller's number being banned and the app being removed from Play.

### Outgoing-text guarantee (shared by all v2.5 sends)
`MainViewModel.sanitizeOutgoing` is the guarantee for every text NOT built by the template fill: invoice text, product messages, custom scheduled text. It applies the bracket rule (any `[...]` seller hint removed whole), the leftover-token rule (any `{...}` token stripped), and the tidy pass — so a seller who types "{price}" into a product description can never leak a placeholder to a customer. Template sends keep the full `fillTemplate` pipeline, unchanged in behavior without a product.

### Migration: Room 2 → 3 (`MIGRATION_2_3`)
CREATE TABLE IF NOT EXISTS only — `invoices`, `invoice_lines`, `products`, `scheduled_messages`. No existing table is altered and no row is touched; column types/defaults match `BusinessEntities.kt` exactly (enums TEXT by name, booleans INTEGER 0/1, Long/Int INTEGER, strings TEXT, defaults UNPAID / 1 / PENDING). No destructive fallback is registered anywhere, so a v1/v2 user's templates, recents, labels, statuses, memos and payments survive the update untouched.

### v2.5 file list (repo-relative)
New: `app/src/main/java/com/fahad1005/sellerchat/data/BusinessEntities.kt`, `app/src/main/java/com/fahad1005/sellerchat/notify/ScheduledMessages.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/InvoicesScreen.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/CatalogScreen.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/ScheduledScreen.kt`. Changed: `app/build.gradle.kts` (version 2.5 / code 10, SDK 36), `gradle/libs.versions.toml` (AGP 8.9.1), `.github/workflows/build.yml` (android-36, Gradle 8.11.1), `app/src/main/java/com/fahad1005/sellerchat/data/Daos.kt`, `app/src/main/java/com/fahad1005/sellerchat/data/Migrations.kt`, `app/src/main/java/com/fahad1005/sellerchat/data/AppDatabase.kt`, `app/src/main/java/com/fahad1005/sellerchat/data/SettingsStore.kt`, `app/src/main/java/com/fahad1005/sellerchat/viewmodel/MainViewModel.kt`, `app/src/main/java/com/fahad1005/sellerchat/MainActivity.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/MainScreen.kt`, `app/src/main/java/com/fahad1005/sellerchat/ui/SettingsScreen.kt`, plus `README.md`, `BUILD_NOTES.md`, `UPDATE_INSTRUCTIONS.md`. Manifest untouched (still exactly POST_NOTIFICATIONS + AD_ID). No new dependency, no new permission, no new asset or font.

## v2.4.1 (privacy-policy URL patch — built on v2.4)

**versionCode 9 / versionName "2.4.1".** One-purpose patch: the in-app Privacy Policy link shipped pointing at the placeholder `https://example.com/sellerchat/privacy-policy`; `PRIVACY_POLICY_URL` in `SettingsScreen.kt` now points at the real hosted page, **`https://fahad1005.github.io/SellerChat/privacy-policy.html`** (GitHub Pages, `docs/` folder of this repo; the policy text ships with the SellerChat GitHub Pages package). That is the only code change — no features, no screens, no dependencies, no permission changes (manifest untouched). The link goes live once both halves are done: this patch installed **and** GitHub Pages enabled on the repo (Settings → Pages → Deploy from a branch → `/docs`); order doesn't matter.

## v2.4 (final bug-fix build — built on v2.3)

**versionCode 8 / versionName "2.4".** Six fixes from user testing, logic/validation/text only: strict per-country number validation, the team-forward upgrade, a sentence-clean template parser, a live-observing Pulse currency, a fragment-proof seed-typo correction, and AdMob banner clearance. **No new permissions** (manifest untouched — still exactly the two v2 lines), **no new dependencies, no new screens, no UI redesign** (state/spacing changes only where a task required them), **no font files** (`res/font/` still exactly one file).

### Task 1 — strict country validation (`PhoneUtils.validateForCountry`)

The bug: Home used the loose `PhoneUtils.normalize`, which *guesses* the country from the digits — so with **+1** selected, a Pakistani `03001234567` was happily re-homed under +1 (or mis-normalized) and WhatsApp opened a wrong/local chat instead of the app ever objecting. The fix treats the dropdown as the truth.

**NSN data** (`Countries.kt`): a per-ISO map of allowed national-significant-number lengths — the digit count after the dial code, trunk `0` excluded — exposed as `Country.nsnLengths`. Core values, exact:

| Country | Dial | NSN |
|---|---|---|
| United States / Canada / Dominican Rep. / Jamaica / Trinidad & Tobago (NANPA) | +1 | 10 |
| Pakistan | +92 | 10 |
| India | +91 | 10 |
| Bangladesh | +880 | 10 |
| Nigeria | +234 | 10 |
| United Kingdom | +44 | 10 |
| UAE | +971 | 9 |
| Saudi Arabia | +966 | 9 |
| France | +33 | 9 |
| Spain | +34 | 9 |
| China | +86 | 11 |

Other listed countries carry their true NSN in the map (a set where fixed-line and mobile lengths differ, e.g. Brazil 10–11, Germany 10–11); any ISO without a pinned entry accepts **7..13** rather than a guessed strict length that could reject real numbers.

**Validator** (`PhoneUtils.validateForCountry(rawInput, country)` → `NumberValidation.Valid(waNumber) / Invalid(reason) / Empty`, pure and deterministic):

1. Digits only; a leading `00` is dropped. Nothing left → **Empty** (no error; actions sit in their normal empty state).
2. Digits starting with the selected country's dial code **and** leaving an allowed NSN length → **Valid**, number used as-is.
3. National form: a leading `0` is the trunk and exactly one is stripped — **except +1 (NANPA), where a 0-leading string is never a valid national number and is measured unstripped** (so `03001234567` under +1 measures 11 ≠ 10 → **Invalid** — the founder's bug case). Valid iff the resulting length is an allowed NSN length; the normalized result is `dial + NSN`.
4. Anything else → **Invalid**.

**Wiring:** `MainViewModel.homeNumberValidation` combines `homeNumber` + `selectedCountry` and recomputes on every change of either. On Home, an Invalid (non-empty) field shows the inline error — exactly **"Invalid number for selected country (+X). Please check the country code."** with the selected dial code — under the number field in the existing error styling, and **disables Open chat, Copy wa.me link and Show QR** (the three number-consuming actions). The send paths use the Valid state's normalized number, so what was validated is what gets sent; Try Demo was switched to the validator too. Tapping an action with an *empty* field keeps the pre-v2.4 "Enter a valid number" hint. **Untouched by design:** Recents open-chat (numbers already stored as E.164), teammate save/launch normalization, and the Share-to-Status own-number field.

### Task 2 — Forward to Team (upgrade of the v2.2 flow, NOT a rebuild)

The Settings teammates storage, the Recents sheet, the picker and the package-targeted launch shipped in v2.2 and are unchanged mechanically (including: forwarding records **no** Recent). Two changes only:

- The sheet action is renamed **"Forward to Team"**.
- The pre-filled message is now exactly: **`Customer Name: {label}, Number: {number}, Context: {template}. Please take over this chat.`** — `{label}` = the Recent's label (fallback: its stored number), `{number}` = the Recent's stored E.164 number, `{template}` = the template title recorded on that Recent (fallback: **"General"**).

### Task 3 — the parser drops whole sentences (`MainViewModel.filledTemplateText`)

The bug: the strip pipeline hollowed sentences out around empty placeholders and left the grammar behind ("The is.", "send to."). The pipeline is now:

1. **Rule-1 fallbacks FIRST, verbatim as shipped in v2.2** — the three order-confirmation total clauses are swapped for complete sentences. They contain no placeholders, so step 2 can never eat them.
2. **Sentence-drop pass:** the text is split into sentences on `.` `!` `?` and the Urdu `۔` (delimiters stay attached; a trailing fragment counts as a sentence). Any sentence still carrying an unfilled **critical** placeholder — `{price}`, `{payment method}`, or `{item}` while the label gave no item — is **dropped whole**. `{name}` is NOT critical (a greeting survives a missing name). **Never-empty guard:** if dropping would leave a blank result, the drop is skipped entirely and step 3 runs on the post-step-1 text.
3. **The existing pipeline, original order:** currency-orphan removal → value substitution → `[...]` strip → leftover `{...}` strip → tidy.

Verified by simulation against the new code (label → output):

1. **"The {item} is Rs {price}." inside EN Price inquiry, label "Ayesha" (no item):** the sentence is dropped → `Hi Ayesha! Thanks for reaching out. Would you like to place an order?` — no "The is."
2. **EN Payment details, unpersonalized (blank label):** the "please send … to …" sentence carries `{price}` + `{payment method}` and is dropped whole → `Share the screenshot here and I'll confirm your order.` — the v2.2 cosmetic cost ("Hi, please send to. …") is gone.
3. **Roman Urdu Order confirm, label "Ayesha — kurta":** the Step-1 fallback sentence survives the drop pass → `Assalam o Alaikum Ayesha, aap ka order confirm ho gaya hai. Total amount confirm karne ke liye reply karein.` (byte-exact the founder's sentence).
4. **A template whose ONLY sentence is "The {item} is Rs {price}.", label "Ayesha":** the drop would empty the message, so the guard skips it and the old strip pipeline runs → `The is.` — imperfect by design, but never an empty send.

### Task 4 — Pulse currency now observes the setting

**Root cause (found, not assumed):** the observation path did not exist. `RecentsScreen` read `val currencyCode = vm.settings.currencyCode` — a bare SharedPreferences getter (`SettingsStore.currencyCode`); **SettingsStore exposed no observable state at all**, and SharedPreferences gives Compose no change signal, so nothing invalidated the Pulse when Settings wrote a new code. The symbol only ever refreshed as a side effect of unrelated recompositions — navigating away and back (route teardown) or the Recents 60-second clock tick — which is exactly the "still shows Rs 0" the testers hit in a quick pass.

**Fix:** `SettingsStore` now backs the value with `_currencyCodeFlow: MutableStateFlow<String>` (initialized once from SharedPreferences, following the same MutableStateFlow pattern the ViewModel already uses for all observable state), exposed read-only as `currencyCodeFlow: StateFlow<String>`; the `currencyCode` setter persists to SharedPreferences **and** updates the flow, so every write — Settings dropdown or the one-time detection pass — publishes. `RecentsScreen` collects it (`collectAsState()`), so the **Pulse "This Week" symbol and the Mark Paid amount prefix** — both read that one collected value — update in the same frame as the Settings change. No restart, no manual refresh. The plain `currencyCode` property remains for one-shot reads (the fill function's orphan-marker lookup).

### Task 5 — seed typo correction, hardened to fragment level

The "aap ka ka order" fix itself is old (v2.2 seedsV3, byte-for-byte default match) — and that matching was its blind spot: a seller who had *edited* their order confirmation kept the defective construction forever, because their body no longer equaled the old default. v2.4 adds a **fragment-level correction** (`SeedData.seedFragmentCorrections()`, applied by a new one-time pass gated on `SettingsStore.seedsV5Done` — a new flag, because seedsV3 is already consumed on installed devices):

- `"ka {item} ka order"` → `"ka order"` (Roman Urdu)
- `"کا {item} کا آرڈر"` → `"کا آرڈر"` (Urdu script — the twin construction)

The pass rewrites the fragment inside **any** stored body containing it. The fragment is never intentional text; everything around it — the seller's own edits included — is preserved byte-for-byte. Fresh-install seeds are unchanged (already correct). Pass order in `MainViewModel.init` is now V2 → V3 → V4 → V5.

### Task 6 — AdMob banner clearance: exactly 12 dp

Home's banner call site changed from `Modifier.padding(horizontal = 16.dp)` to `Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)`. The 12 dp sits on the banner container itself — fixed layout space **outside** the scroll area — so the bottom of the scroll content (the "Open chat" CTA zone) can never rest flush against the ad in any scroll or keyboard state. No other layout changed.

### Size statement

The v2.4 delta is code only (a validation table, a validator, parser steps already counted in lines above) — no dependencies, no fonts, no assets. The < 5 MB budget stands as CI measures it: the run prints both `app-release.apk` and `app-release.aab` byte counts, and both must stay under 5,000,000 (see UPDATE_INSTRUCTIONS.md).

### v2.4 acceptance additions (verified at CI / on phone — see UPDATE_INSTRUCTIONS.md)

| # | Criterion | How |
|---|---|---|
| 31 | +1 selected + a 0-leading 11-digit number → inline country error, Open chat / Copy link / Show QR disabled; nothing reaches WhatsApp | Phone check in UPDATE_INSTRUCTIONS |
| 32 | +92 selected + `0300…` passes validation and opens the correct chat with the validator's normalized number | Phone check in UPDATE_INSTRUCTIONS |
| 33 | Forward to Team sends the exact new message format (label / number / template context, "General" fallback) | Phone check in UPDATE_INSTRUCTIONS |
| 34 | Parser: critical-placeholder sentences drop whole; the founder's Roman Urdu fallback sentence survives; a single-sentence template still sends (guard) | Simulations above + phone checks |
| 35 | Settings currency change flips the Pulse symbol (and the Mark Paid prefix) immediately, no restart | Phone check in UPDATE_INSTRUCTIONS |
| 36 | The fragment correction reaches EDITED order-confirmation templates; surrounding edits preserved | Correct a stored body containing the fragment, relaunch |
| 37 | Banner never touches the CTA zone — 12 dp clearance in every scroll/keyboard state | Phone check in UPDATE_INSTRUCTIONS |

## v2.3 (Global Smart Localization — built on v2.2)

**versionCode 7 / versionName "2.3".** One-time first-launch region detection, a sovereign manual **Region & Currency** settings section, a persisted currency code, and 12 new seed templates (French / Spanish / Arabic / Chinese). No new permissions (manifest untouched — still exactly the two v2 lines), no new dependencies, no new font files (`res/font/` still contains exactly one file: the Urdu Nastaliq subset).

> **Founder Task 4 — verified, NOT rebuilt.** The bracket stripping, the "ka ka" seed fix and the empty-price currency-orphan removal from the v2.3 brief all shipped in **v2.2**. The v2.2 `filledTemplateText` rules were re-read line by line for this build and survive unchanged in substance: bracket-segment stripping, leftover `{...}` stripping (including multi-word tokens), the currency-orphan pair of rules, the three empty-price sentence fallbacks (EN / RU / UR), and the tidy pass. The only parser change in v2.3 is additive: the orphan marker set now also covers `$`, `£`, `€`, plus the symbol of the seller's configured currency code (regex-quoted before use).

### One-time detection — precedence and sovereignty

The pass lives in `MainViewModel.initializeRegionIfNeeded()` and runs from `resolveInitialCountry()`, before Home or First Run can read a country. It is gated by the new `SettingsStore.regionInitialized` flag:

| Value | Source, in precedence order | Persisted to |
|---|---|---|
| Country | 1) `TelephonyManager.getSimCountryIso()` (no permission needed for the SIM country) 2) `Locale.getDefault().country` 3) the previously saved country 4) the built-in fallback (PK) | the existing `countryIso` setting |
| Currency code | the detected country through `CurrencyUtils.codeForCountry()`: the hand table wins (PK→PKR, IN→INR, BD→BDT, NG→NGN, AE→AED); every other country resolves via `java.util.Currency.getInstance(Locale("", iso))`; any failure → USD | the new `currencyCode` setting |
| Preferred template language | the device Locale language mapped through the table below; unsupported → **EN, set explicitly** so Home prioritizes English | the existing `preferredTemplateLanguage` setting |

Locale language map: `ur→UR · hi→HI · ta→TA · te→TE · mr→MR · fr→FR · es→ES · ar→AR · zh→ZH · en→EN · anything else→EN`.

Once the flag is set the pass never runs again — including for upgraders, who get exactly one detection pass on their first v2.3 launch and full manual control after it. Every lookup is wrapped: an exotic SIM/Locale combination degrades to the saved country / USD / EN fallbacks and can never crash first launch. Only permission-free APIs are used — no location, no account, no network.

### Region & Currency settings section

Settings now groups all three controls under one **Region & Currency** heading, one UI instance each, no duplicated controls:

- **Country** — the pre-existing default-country picker and storage key, moved under the section (no data migration).
- **Currency** — NEW dropdown over the persisted `currencyCode`: PKR (Rs), INR (₹), BDT (৳), USD ($), GBP (£), EUR (€), AED (AED), SAR (SAR), NGN (₦). The choice drives the Recents "Mark Paid" amount prefix, the Pulse "This Week" total (Indian digit grouping now follows the *currency* — INR groups lakh/crore style), and the fill function's dynamic orphan marker. Amount formatting by country still exists for country-only call sites and now flows through the same code/symbol functions.
- **Preferred Template Language** — the pre-existing setting and key, moved (not copied) into the section; its dropdown now also lists French, Spanish, Arabic and Chinese.

Settings deliberately stay in **SharedPreferences** (`SettingsStore`), the settings engine since v1. Migrating to DataStore would add a dependency and a data-migration risk to carry three scalars; the brief's "save in DataStore" is satisfied in substance — durable local persistence with manual override — at zero size and zero migration risk. This is a deliberate no-risk choice, recorded here.

### New seed languages (FR / ES / AR / ZH)

`TemplateLanguage` gains `FR, ES, AR, ZH` (the v1 enum names in the database are untouched). `SeedData.v4Templates()` adds **3 seeds per language** (Price / Payment / Delivery), mirroring the English structure and placeholders — `{name}`, `{item}`, `{price}`, and `{payment method}` in the Payment seeds, which the v2.2 parser strips when unpersonalized. Market writing conventions are native-style: French and Spanish put the amount before a postposed `€`; Arabic avoids a single market's currency noun and phrases amounts generically ("المبلغ …" / "السعر: …"); Chinese writes `{price} 元`.

**All 12 new bodies are AI drafts pending native review** — flagged here, in `SeedData` comments and in the README, exactly like the Tamil / Telugu / Marathi drafts. They are starting copy for a native speaker to correct, not verified translations.

**Rendering:** Arabic templates render **RTL with the system font** (a new `usesRtlLayout()` in `TemplateText.kt`; previews, list rows and the editor). `usesNastaliq()` still keys on `language == UR` only — the bundled font is never used for Arabic or any other language. Chinese, French and Spanish render LTR in system fonts. No font files were added.

**Seed-pass chaining proof** (`MainViewModel.init`, top to bottom): (1) v1 seeds when the table is empty → (2) `seedsV2Done` pass (v2 languages) → (3) `seedsV3Done` corrections (v2.2 wording) → (4) `seedsV4Done` pass (this build). An upgrader from v2.0 or v2.1 therefore runs V2 → V3 → V4 **in that order** on their first v2.3 launch. The V4 pass additionally guards on title+language: a candidate already present in the library is skipped, so even a lost flag cannot duplicate the new seeds.

**Filter tabs and pickers:** the Templates filter row gains French · Spanish · العربية · 中文 (the row already scrolls horizontally), and both language dropdowns (Settings preferred language, the template editor's language picker) list the four new languages. Home's chip ordering needs no change: an explicitly preferred language — now including the detected FR/ES/AR/ZH — already leads the chip row, and the v2.1 country-derived fallback still applies when the preference is "Any".

### Size statement

The entire v2.3 delta is code plus **seed text** (12 short strings) — no dependencies, no fonts, no assets. Nothing in this build can move the release size by more than a few KB, and the < 5 MB budget stands as CI measures it: the run prints both `app-release.apk` and `app-release.aab` byte counts, and both must stay under 5,000,000 (see UPDATE_INSTRUCTIONS.md).

### v2.3 acceptance additions (verified at CI / on phone — see UPDATE_INSTRUCTIONS.md)

| # | Criterion | How |
|---|---|---|
| 25 | Fresh install: country, currency and preferred language arrive pre-detected (SIM first, Locale fallback) | Phone check: install on a reset phone, open First Run / Settings → Region & Currency |
| 26 | Detection runs once only; manual Region & Currency choices survive relaunch and are never re-detected over | Phone check: change all three, force-stop, relaunch, re-read Settings |
| 27 | Changing Currency changes the Mark Paid prompt symbol and the Pulse total symbol (INR groups Indian-style) | Phone check in UPDATE_INSTRUCTIONS |
| 28 | French / Spanish / Arabic / Chinese templates appear under their Templates tabs; Arabic rows render right-aligned RTL, Chinese LTR | Phone check in UPDATE_INSTRUCTIONS |
| 29 | The v2.2 fill guarantees still hold (no raw brackets/tokens, no orphaned currency, no "ka ka") | The v2.2 phone checks in the previous UPDATE_INSTRUCTIONS still apply |
| 30 | Install-OVER proof continues: v2.3 installs over the previous version, data intact, both phones | UPDATE_INSTRUCTIONS Section 3 |

## v2.2 (final text fixes + Forward to Teammate — built on v2.1)

**versionCode 6 / versionName "2.2".** No UI/design changes beyond the two Task 3 additions, no new permissions (manifest untouched — still exactly the two v2 lines), no new dependencies. **v2.2 SUPERSEDES the planned v2.1.1 bump**: it is the build that runs the install-over proof over a working v2.1 (same fixed key, higher code).

> ⚠️ **FOUNDER VETO POINT — the Urdu twin fix.** Task 1 named only the Roman Urdu order-confirmation, but the Urdu-script order confirmation (#8) had the *identical* construction ("آپ کا {item} کا آرڈر" → renders "آپ کا کا آرڈر" when the item is blank) and received the identical fix (the `{item}` slot removed). It is flagged here and in UPDATE_INSTRUCTIONS — if you want the Urdu seed kept as it was, say so and it reverts in one edit; the Roman Urdu fix stands either way.

### Task 1 — the "ka ka" seed fix

The doubling was structural, not a typo in one word: the Roman Urdu body read "aap ka **{item}** ka order confirm ho gaya hai", and with an empty `{item}` the two "ka"s land side by side. No wording that keeps `{item}` in that slot can read correctly in both the filled and empty cases, so `{item}` leaves the template:

- **#11 (Roman Urdu):** "Assalam o Alaikum {name}, aap ka {item} ka order confirm ho gaya hai. Total Rs {price} hai." → **"Assalam o Alaikum {name}, aap ka order confirm ho gaya hai. Total Rs {price} hai."** The required end-state phrase "aap ka order" now holds whether or not the label carried an item. (Trade-off, accepted by the task: the item no longer appears in this one template — the other five order/price templates still use it.)
- **#8 (Urdu, the twin):** "السلام علیکم {name}، آپ کا {item} کا آرڈر کنفرم ہو گیا ہے۔ کل رقم {price} روپے ہے۔" → **"السلام علیکم {name}، آپ کا آرڈر کنفرم ہو گیا ہے۔ کل رقم {price} روپے ہے۔"**

### Task 2 — the fill function, hardened (`MainViewModel.filledTemplateText`)

Still ONE shared function behind the preview and every send (call sites unchanged: Home `builtText()`, Home preview, `tryDemo`). Rules run in this order, all deterministic string work — no grammar engine:

1. **Empty-price sentence fallbacks.** There is no price input in the app, so `{price}` always resolves empty; the order-confirmation total clauses are swapped wholesale for complete sentences, three explicit shapes:
   - EN "Total: Rs {price}." → "I'll confirm the total amount on reply."
   - RU "Total Rs {price} hai." → "Total amount confirm karne ke liye reply karein." *(the founder's exact words)*
   - UR "کل رقم {price} روپے ہے۔" → "کل رقم جواب میں کنفرم کر دوں گا۔"
2. **Currency-orphan rule.** A currency marker (`Rs`, `Rs.`, `₹`, `روپے`) immediately beside the `{price}` slot — either side — is removed with the placeholder.
3. **Bracket rule.** Any remaining `[...]` segment is removed whole, brackets and contents ("[insert]", "[X]"). Seed edits (below) mean the payment templates no longer rely on this rule, but user templates and the older v2 language seeds still can carry brackets.
4. **Leftover-token rule** (v2.1, extended). Any remaining `{...}` token is stripped — now including multi-word tokens like `{payment method}`.
5. **Tidy pass.** Runs of spaces collapse to one; a space before `. , ! ? ، ۔` is removed; ends trimmed.

**Seed edits for Task 2** (fresh installs; the correction pass below handles existing libraries): the three v1 payment templates write `{payment method}` instead of a bracketed hint — #3 "[your payment method]", #9 "[payment method]", #12 "[payment method]".

**Before → after, exactly as the new code renders them** (label shown in brackets):

- **#11, label "Ayesha — kurta" (item present, no price):**
  Before: "Assalam o Alaikum Ayesha, aap ka kurta ka order confirm ho gaya hai. Total Rs hai."
  After: "Assalam o Alaikum Ayesha, aap ka order confirm ho gaya hai. Total amount confirm karne ke liye reply karein."
- **#3 EN payment, nothing filled (blank label):**
  Before: "Hi , please send Rs to [your payment method]. Share the screenshot here and I'll confirm your order."
  After: "Hi, please send to. Share the screenshot here and I'll confirm your order."
- **#2 EN order confirmation, label "Ayesha", no price:**
  Before: "Hi Ayesha, your order for is confirmed. Total: Rs . I'll share the delivery details shortly."
  After: "Hi Ayesha, your order for is confirmed. I'll confirm the total amount on reply. I'll share the delivery details shortly."
- **#8 Urdu order confirmation, blank label:**
  Before (old seed): "السلام علیکم ، آپ کا کا آرڈر کنفرم ہو گیا ہے۔ کل رقم روپے ہے۔" *(the doubled کا, plus "کل رقم روپے" with the price missing)*
  After: "السلام علیکم، آپ کا آرڈر کنفرم ہو گیا ہے۔ کل رقم جواب میں کنفرم کر دوں گا۔"
- **#5 Out of stock (bracket rule), label "Ayesha — kurta":** "It will restock in [X] days." → "It will restock in days."

### The seed correction pass (why it exists)

Editing `SeedData` alone changes nothing for existing installs — seeds only run on an empty table. So v2.2 adds a one-time **seedsV3 pass** (flag `seedsV3Done` in SettingsStore, same pattern as `seedsV2Done`): for each old→new pair in `SeedData.seedCorrections()`, a stored row is updated **only if its body still equals the old default byte-for-byte** — a template the user has edited is never clobbered. Fresh installs seed the corrected bodies directly and the pass finds nothing to do.

Old → new pairs (5): #11 and #8 as in Task 1 above; #3 "…to [your payment method]. Share…" → "…to {payment method}. Share…"; #9 "…{price} روپے [payment method] پر بھیجیں…" → "…{price} روپے {payment method} پر بھیجیں…"; #12 "…Rs {price} [payment method] par bhejein…" → "…Rs {price} {payment method} par bhejein…". (Full literal pairs live in `SeedData.seedCorrections()`; a build-time check verified each new body appears byte-identically in both the seed list and the pair list.)

### Task 3 — Forward to Teammate

- **Storage:** up to 3 teammates in SharedPreferences via `SettingsStore` (slots `team_name_1..3` / `team_number_1..3`). Deliberately NOT a Room entity — three name/number pairs are scalars, and this keeps the Room schema (and its migration burden) untouched. The number is stored as entered; it is normalized with the default country (`PhoneUtils.normalize`) only at launch time.
- **Settings:** a "Team Members" section in the existing Settings screen, styled exactly like the sections around it (titleSmall heading, OutlinedTextField + OutlinedButton): saved teammates listed with Delete; an add form (Name + Phone) shown while fewer than 3 are saved; validation is name non-blank and number ≥ 7 digits, with an inline hint otherwise.
- **Recents:** the row bottom sheet gains a fifth action, **"Forward to Teammate"**, after Mark Paid (the original four are byte-unchanged). It swaps the sheet's content for the teammate list; with none saved it shows "No teammates yet — add them in Settings" and an **Open Settings** action (wired through a new `onOpenSettings` callback from MainScreen to the existing Settings route).
- **Launch:** the teammate's normalized number is opened through the same package-targeted `openChat` path — Settings default app first, then the current selection, then the other installed app — with the pre-filled text EXACTLY: `Customer: X. Please take over this chat.` where X is the recent's label if non-blank, else the recent's stored E.164 number. If every launch fails, the sheet shows Home's existing failure copy ("Could not open WhatsApp. Try the other app."). **Forwarding records no Recent and changes no CRM state** — the teammate is staff, not a customer; the customer row keeps its own status/memo/follow-up untouched.

### v2.2 acceptance additions (verified at CI / on phone — see UPDATE_INSTRUCTIONS.md)

| # | Criterion | How |
|---|---|---|
| 20 | "ka ka" gone in RU (and UR) order confirmations, filled or empty | Phone check: send #11 with a plain-name label and read the WhatsApp draft |
| 21 | No raw `[...]` or `{...}` ever reaches a customer | Phone check: payment template draft shows no brackets/braces |
| 22 | Empty price never strands "Rs" / "روپے"; the three total clauses read as full sentences | Phone check: the three order-confirmation drafts |
| 23 | Forward to Teammate lands on the TEAMMATE's chat with the exact hand-over text; no new Recent appears | Phone check in UPDATE_INSTRUCTIONS |
| 24 | Install-OVER proof (supersedes the v2.1.1 plan): v2.2 installs over working v2.1, test Recent survives, both phones | UPDATE_INSTRUCTIONS Section 3 |

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
