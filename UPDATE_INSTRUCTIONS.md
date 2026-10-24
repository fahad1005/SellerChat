# SellerChat — v2.6 Update Steps (read this first — v2.6 SUPERSEDES v2.5)

Fahad — upload **v2.6, not v2.5**. v2.5 was never installed, and v2.6 contains everything v2.5 had plus the winner pack: **full khata ledger** (udhaar / payment / advance per customer, partial payments, due-date reminders, shareable statements), **multi-business** (run several shops in one app — money books are per shop, customers/templates/scheduled are shared; your existing data opens under "My Business" with zero action), a **Money tab** (Today card: sales, owed, expenses, profit), a **Today card on Sell**, **auto follow-up suggestions** (waiting 2+ days + overdue khata, no setup), **multichannel open-chat** (Telegram / Messenger / SMS links — opens those apps, does NOT receive messages inside SellerChat), an **inline discount calculator**, a **Share-my-shop card + ad link**, and **delivery stages** (Packed, Delivered). It installs **over v2.4.1** (or over any earlier version) — no uninstall. Your Recents, templates, statuses, memos, invoices and products all survive: the database change adds new tables and one new column (businessId, defaulted) on three existing tables — nothing is deleted or rewritten.

**What v2.6 deliberately does NOT do:** it does not receive messages inside the app, and nothing sends itself — Telegram/Messenger open those apps without a pre-filled message, and reminders open WhatsApp pre-filled for your tap. Anyone promising a free in-app WhatsApp inbox is selling a ban risk.

## Section 1 — Upload the v2.6 files

1. Unzip the v2.6 package.
2. In the SellerChat repo, **Add file → Upload files** and drag in everything from the unzipped folder. You should see **20 files** (paths are repo-relative, so files land on top of their old versions):
   - `app/build.gradle.kts` (version 2.6 / code 11, Play API 36)
   - `app/src/main/java/com/fahad1005/sellerchat/data/KhataEntities.kt` (NEW — businesses + khata + channel handles + expenses)
   - `app/src/main/java/com/fahad1005/sellerchat/notify/KhataReminders.kt` (NEW — due-date reminders)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/MoneyScreen.kt` (NEW — Money tab + khata detail)
   - `app/src/main/java/com/fahad1005/sellerchat/data/Entities.kt` (Packed, Delivered statuses)
   - `app/src/main/java/com/fahad1005/sellerchat/data/BusinessEntities.kt` (businessId on invoices/lines/products)
   - `app/src/main/java/com/fahad1005/sellerchat/data/Daos.kt` (BusinessDao + per-shop queries)
   - `app/src/main/java/com/fahad1005/sellerchat/data/AppDatabase.kt` (database version 4)
   - `app/src/main/java/com/fahad1005/sellerchat/data/Migrations.kt` (migration 3 → 4)
   - `app/src/main/java/com/fahad1005/sellerchat/data/SettingsStore.kt` (selected shop)
   - `app/src/main/java/com/fahad1005/sellerchat/viewmodel/MainViewModel.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/MainActivity.kt` (khata reminder deep link)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/MainScreen.kt` (Sell / Customers / Money tabs)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt` (Today card, discount, channel picker)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt` (delivery stages, Follow-up-today)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/SettingsScreen.kt` (Manage businesses)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/TemplatesScreen.kt` (Back button)
   - `README.md`, `BUILD_NOTES.md`, `UPDATE_INSTRUCTIONS.md`
   - There is **no** workflow file and **no** signing material in the zip — `build.yml` is unchanged since v2.4.1 (already Android 36) and your keys stay in repo secrets only. If the uploader skips nothing, fine; if it ever skips a file, re-upload that one file.
3. Commit. Watch Actions: the build must print **`SIGNING: FIXED KEY`** and the release APK must be **under 5,000,000 bytes** (report both sizes). If the build goes red, send the first error text — do not guess.

## Section 2 — Install OVER your current version (do NOT uninstall)

1. Install the v2.6 **app-release.apk** straight over v2.4.1 — **no uninstall** (same permanent key). Your Recents, templates, invoices, products and statuses must all still be there, now under the "My Business" shop in the Money tab.
2. If the app crashes on first open, or any old data is missing: stop, do NOT uninstall (uninstalling deletes the data we need to diagnose), and send the exact error or what is missing.

## Section 3 — v2.6 phone checks (one per feature)

- **Old data survives:** Recents, templates, invoices, products all present; Money tab header shows "My Business".
- **Khata:** Money → "Add entry for a customer" → pick a customer → add Udhaar 1000, then Payment 400 (Cash) → balance reads "To receive 600". Add an Advance 200 → balance reads "To receive 400". Share statement → clean text with all entries + balance.
- **Due reminder:** add Udhaar with due "Tomorrow" + "Remind me" on → allow notifications → (due-date behavior; the in-app due badge must show immediately).
- **Multi-business:** Settings → Manage businesses → Add shop "Food" (PKR) → Money header switches to Food → khata/invoices/products show empty for Food, full for My Business. Switch back. Delete "Food" (empty) → allowed. Try deleting "My Business" (has records) → refused with an honest note.
- **Today card:** Sell tab top card shows "Today · My Business" + owed figure matching the khata; tap Owed → Money tab; tap Waiting → Customers tab.
- **Auto follow-ups:** Customers tab shows "Follow up today" for a customer waiting 2+ days (change a test customer's status date by waiting, or use an overdue khata entry) → Send opens WhatsApp pre-filled.
- **Multichannel:** Sell → "Send on another app" → SMS → messaging app opens with text ready. Telegram with a username → Telegram opens (message NOT pre-filled — that is expected, the app says so). Telegram with a missing app → WhatsApp opens instead + note.
- **Discount:** pick a product (Rs 500) → type 10% → preview shows "Rs 500 − 10% = Rs 450" and the sent template uses 450.
- **Share my shop:** Money → Share my shop → enter your number + source "Instagram" → share → text lists products + prices + a wa.me link with "I saw your ad on Instagram".
- **Delivery stages:** tap a customer's status chip repeatedly → Inquiry → Ordered → Packed → Shipped → Delivered → Paid → Inquiry.
- **Templates still reachable:** Sell → Business tools → Templates → list opens; Back returns.

# SellerChat — v2.5 Update Steps (read this first)

Fahad — v2.5 is the **business-tools build**: **Invoices** (bill book with Unpaid/Paid + Mark Paid that feeds Pulse), **Product catalog** (your price list; pick a product on Home and it fills {item} and {price}), **Send photo/file to unsaved numbers** (WhatsApp opens with the file attached — you tap send there), **Scheduled messages** (a reminder at the time you pick — nothing sends itself), **Contact import + clipboard number chip** (no new permissions), and **assisted Greeting / Away** (suggested texts for new customers and closed hours — NOT auto-reply; nothing sends in the background). It installs **over** v2.4.1, no uninstall — your Recents, templates, statuses and memos all survive (the database change only adds new tables).

## Section 1 — Upload the v2.5 files

1. Unzip the v2.5 package.
2. In the SellerChat repo, **Add file → Upload files** and drag in everything from the unzipped folder. You should see **21 files** (paths are repo-relative, so files land on top of their old versions):
   - `app/build.gradle.kts` (version 2.5 / code 10, Play API 36)
   - `gradle/libs.versions.toml` (AGP 8.9.1)
   - `.github/workflows/build.yml` (Android 36 tools, Gradle 8.11.1) — if the uploader skips the `.github` folder (it did before), nothing is lost: this workflow is identical to the one already in the repo from v2.4.1. Only paste it via Add file → Create new file if the repo's workflow does NOT mention android-36.
   - `app/src/main/java/com/fahad1005/sellerchat/data/BusinessEntities.kt` (NEW — invoices, products, scheduled messages)
   - `app/src/main/java/com/fahad1005/sellerchat/data/Daos.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/data/Migrations.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/data/AppDatabase.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/data/SettingsStore.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/viewmodel/MainViewModel.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/notify/ScheduledMessages.kt` (NEW)
   - `app/src/main/java/com/fahad1005/sellerchat/MainActivity.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/ui/InvoicesScreen.kt` (NEW)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/CatalogScreen.kt` (NEW)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/ScheduledScreen.kt` (NEW)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/ui/MainScreen.kt`
   - `app/src/main/java/com/fahad1005/sellerchat/ui/SettingsScreen.kt`
   - `README.md`, `BUILD_NOTES.md`, `UPDATE_INSTRUCTIONS.md` (this file)
   - There is **no signing material** in the package — your keys stay in repo secrets only.
3. **Commit changes.** The build starts by itself.

## Section 2 — Build and read the signing line (same as before)

1. **Actions** tab → open the finished **Build APK** run.
2. On the run page it must say **`SIGNING: FIXED KEY`** ✅ — anything else, stop and tell Bullet. A fallback build cannot install over your current app.
3. The same step prints `app-release.apk -> N bytes` and `app-release.aab -> N bytes`. **Both must be under 5,000,000.** Tell Bullet both numbers.
4. Green tick → **Artifacts → SellerChat-apks** → download → unzip → use **app-release.apk**.

## Section 3 — Install OVER v2.4.1 (do NOT uninstall — your data is the test)

Do it on **Phone 1**, then **Phone 2**:

a. On the phone, open SellerChat (v2.4.1) → Recents → confirm your test customer is there. That's the data worth protecting.
b. Install the v2.5 **app-release.apk** straight over it — **no uninstall** (same permanent key).
c. Success = two things, both: **it installs** AND **your test Recent is still there**, with its status/memo intact. If anything is missing, tell Bullet before touching Phone 2.

## Section 4 — v2.5 phone checks (one per feature)

1. **Invoices:** Home → **Business tools → Invoices → New invoice** → pick a customer, add a line (item + amount) → Save → open it → **Send via WhatsApp** → WhatsApp opens with the invoice text (number, lines, total) typed, nothing sent by itself. Back in the app → **Mark Paid** → the invoice moves to Paid, and that customer in Recents shows **Paid** with the amount.
2. **Catalog:** **Business tools → Catalog → Add product** (name + price) → back on **Home**, the product appears as a chip → tap it → the preview fills {item} and {price} → **Open chat** sends exactly the previewed text.
3. **Send photo/file:** Home → type a valid number → **Send photo/file** → pick a photo → WhatsApp opens **with the photo attached** to that number's chat. (If WhatsApp opens without it, the app says so — attach it in WhatsApp and send.)
4. **Scheduled:** **Business tools → Scheduled → Schedule a message** → pick a customer + a template + a time 2 minutes from now → allow the notification when asked → at that time a notification arrives → tap it → WhatsApp opens with the message typed → the item shows **Sent**. Nothing sends without your tap.
5. **Clipboard + contacts:** copy a phone number from another app → return to SellerChat Home → a chip offers **"Number from clipboard — Use it"** → tap it → the number field fills. **Import from contacts** → pick a contact → number + name fill in.
6. **Greeting / Away:** **Settings → Greeting & Away** → turn on **Greeting** → Home → type a brand-new number → a **"New customer — send the greeting?"** card appears → **Use greeting** → the preview shows the greeting → send it. For Away: set hours that exclude right now, turn Away on → Home shows the **Away mode** banner with **Send away message**.

---

# SellerChat — v2.4.1 Update Steps (read this first)

Fahad — v2.4.1 is a **tiny one-purpose patch**: it points the app's **Privacy Policy** link at the real hosted page (**fahad1005.github.io/SellerChat/privacy-policy.html**) instead of the placeholder it shipped with. Nothing else changes — no features, no screens, no signing changes. It installs **over** v2.4, no uninstall.

## Section 1 — Upload the v2.4.1 files

1. Unzip **SellerChat-v2.4.1-updated-files.zip**.
2. In the SellerChat repo, **Add file → Upload files** and drag in everything from the unzipped folder. You should see **4 files** (2 that change the app, 2 docs):
   - `app/src/main/java/com/fahad1005/sellerchat/ui/SettingsScreen.kt` (the privacy-policy URL — nothing else in the file changed)
   - `app/build.gradle.kts` (version 2.4.1 / code 9)
   - `UPDATE_INSTRUCTIONS.md` (this file) and `BUILD_NOTES.md` (the v2.4.1 entry)
   - There is **no** workflow file and **no signing material** in the zip.
3. **Commit changes.** The build starts by itself.

## Section 2 — Build and install (same as before)

1. **Actions** tab → open the finished **Build APK** run → the run page must say **`SIGNING: FIXED KEY`** ✅. Anything else, stop and tell Bullet.
2. **Artifacts → SellerChat-apks** → download → use **app-release.apk**.
3. Install it **straight over** v2.4 on both phones — **no uninstall**, your Recents and templates stay put.

## Section 3 — The one check

1. In the app: **Settings → Privacy policy** (the link near the bottom). It should open **fahad1005.github.io/SellerChat/privacy-policy.html** in the browser.

> **Order doesn't matter:** the in-app link only starts working once **both** things are done — (a) this patch is installed, and (b) **GitHub Pages is enabled** on the repo (in the *other* zip: upload the `docs/` folder, then repo **Settings → Pages → Build and deployment → Deploy from a branch → Branch: main, folder `/docs`** → Save). Do them in either order; the link goes live when the second one lands.

---
---

# SellerChat — v2.4 Update Steps (kept for reference)

Fahad — v2.4 is the **final bug-fix build** from user testing. Six fixes, logic only — no new screens, no design changes: **strict country-code validation** (a Pakistani number typed under +1 is now caught in the app, not in WhatsApp), **Forward to Team** with a proper hand-over message, a **parser that removes whole broken sentences** instead of leaving "The is." behind, the **Pulse bar now follows your currency setting instantly**, the "ka ka" fix now reaches **even templates you edited by hand**, and a **12 dp safety gap** between the Open chat button and the ad banner. Nothing about signing changes — your permanent key does all the work, and this build installs **over** v2.3.

> **Which zip is this?** A normal diff zip, on purpose: your repo is at the **v2.3 final source** (that's what the 15-file FULL upload committed), so this zip's 10 files land exactly on top of it. No cumulative re-pack needed this time.

---

## Section 1 — Upload the v2.4 files

1. Unzip **SellerChat-v2.4-updated-files.zip**.
2. In the SellerChat repo, **Add file → Upload files** and drag in everything from the unzipped folder. Paths are repo-relative, so files land on top of their old versions. You should see **10 files**:
   - `app/build.gradle.kts` (version 2.4 / code 8)
   - `app/src/main/java/com/fahad1005/sellerchat/data/Countries.kt` (per-country number-length table)
   - `app/src/main/java/com/fahad1005/sellerchat/util/PhoneUtils.kt` (the strict validator)
   - `app/src/main/java/com/fahad1005/sellerchat/viewmodel/MainViewModel.kt` (live validation state, sentence-clean parser, forward message, fragment-correction pass)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt` (inline country error + disabled actions, banner gap)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt` (Pulse follows the currency setting, "Forward to Team")
   - `app/src/main/java/com/fahad1005/sellerchat/data/SettingsStore.kt` (observable currency + correction flag)
   - `app/src/main/java/com/fahad1005/sellerchat/data/SeedData.kt` (the fragment corrections)
   - `BUILD_NOTES.md` and this file
   - There is **no** workflow file and **no signing material** in the zip — `build.yml` is unchanged and your keys stay in repo secrets only.
3. **Commit changes.** The build starts by itself.

---

## Section 2 — Build and read the signing line (same as before)

1. **Actions** tab → open the finished **Build APK** run.
2. On the run page (job summary / "Report build sizes and signing mode" step) it must say:
   - **`SIGNING: FIXED KEY`** ✅ — anything else, stop and tell Bullet. A fallback build cannot install over your current app.
3. The same step prints `app-release.apk -> N bytes` and `app-release.aab -> N bytes`. **Both must be under 5,000,000.** Tell Bullet both numbers.
4. Green tick → **Artifacts → SellerChat-apks** → download → unzip → use **app-release.apk**.

---

## Section 3 — Install OVER v2.3 (do NOT uninstall — your data is the test)

Do it on **Phone 1**, then **Phone 2**:

a. On the phone, open SellerChat (v2.3) → Recents → confirm your test customer is there. That's the data worth protecting.
b. Install the v2.4 **app-release.apk** straight over it — **no uninstall** (same permanent key).
c. Success = two things, both: **it installs** AND **your test Recent is still there**, with its status/memo intact. That survival is itself one of this build's checks — if anything is missing, tell Bullet before touching Phone 2.

---

## Section 4 — v2.4 phone checks (the six fixes)

1. **Wrong country is caught (the big one):** Home → set the country to **United States (+1)** → type `03001234567` → an inline error appears under the number: **"Invalid number for selected country (+1). Please check the country code."** — and **Open chat, Copy wa.me link and Show QR are all disabled**. Nothing can be sent to WhatsApp.
2. **Right country passes:** switch the country to **Pakistan (+92)** → type `0300 1234567` → the error clears, **Open chat** lights up → tap it → WhatsApp opens the correct chat for **+92 300 1234567**.
3. **Forward to Team:** Recents → tap a customer → the sheet's last action is now **Forward to Team** → tap it → tap a teammate → WhatsApp opens **on the teammate's number** with this exact message typed: **"Customer Name: \<label\>, Number: \<the customer's number\>, Context: \<the template you used\>. Please take over this chat."** (A customer with no template recorded shows **Context: General**.) Forwarding still adds **no** new Recent.
4. **Clean sentences, no broken grammar:** label just `Ayesha` → tap **Payment details** → the preview is now just **"Share the screenshot here and I'll confirm your order."** — the old broken "Hi, please send to." sentence is *gone entirely*, not patched. Then label `Ayesha — kurta` → **Order confirm (Roman)** → preview reads "…aap ka order confirm ho gaya hai. Total amount confirm karne ke liye reply karein." — clean, exactly as before.
5. **Pulse follows currency instantly:** Settings → **Region & Currency → Currency** → pick **USD ($)** → go straight to **Recents** (do NOT restart the app) → the top line's **This Week** now shows **$ 0**, not Rs 0. Tap a customer → **Mark Paid** → the amount box also starts with **$**. Change the currency back to PKR when you're done.
6. **Banner gap:** Home → look at the bottom: there is now a clear visible gap between the buttons and the ad strip — the ad never touches the Open chat area, scrolled or not, keyboard open or not.

## Later (not now)

Real AdMob IDs and the $25 Play Console are still TODO(FAHAD) in the README. The AI-drafted seed translations (Tamil / Telugu / Marathi / French / Spanish / Arabic / Chinese) are still pending a native-speaker once-over before a public launch.

---
---

# SellerChat — v2.3 Update Steps (kept for reference)

Fahad — v2.3 is **Global Smart Localization**: the first time it opens, SellerChat detects your country, currency and preferred template language **once** — after that, your manual choices in the new **Settings → Region & Currency** section are in charge forever. It also adds 12 templates: French, Spanish, Arabic and Chinese (price / payment / delivery in each). Nothing about signing changes — your permanent key from v2.1 does all the work.

> The founder brief's "Task 4" fixes (raw brackets, "ka ka", empty-price leftovers) are **not** new in v2.3 — they shipped in v2.2 and are verified still in place. Section 4 below re-checks one of them so you can see they survived.

---

## Section 1 — Upload the v2.3 files

1. Unzip **SellerChat-v2.3-updated-files.zip**.
2. In the SellerChat repo, **Add file → Upload files** and drag in everything from the unzipped folder. Paths are repo-relative, so files land on top of their old versions. You should see **14 files**:
   - `app/build.gradle.kts` (version 2.3 / code 7)
   - `app/src/main/java/com/fahad1005/sellerchat/data/Entities.kt` (4 new template languages)
   - `app/src/main/java/com/fahad1005/sellerchat/data/SeedData.kt` (the 12 new templates)
   - `app/src/main/java/com/fahad1005/sellerchat/data/SettingsStore.kt` (region flag + currency code + seed flag)
   - `app/src/main/java/com/fahad1005/sellerchat/util/CurrencyUtils.kt` (currency codes + symbols)
   - `app/src/main/java/com/fahad1005/sellerchat/viewmodel/MainViewModel.kt` (one-time detection + seed pass + parser extension)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/SettingsScreen.kt` (Region & Currency section)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/TemplatesScreen.kt` (French / Spanish / العربية / 中文 tabs)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt` (amounts use your chosen currency)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/components/TemplateText.kt` (Arabic right-to-left rendering)
   - `BUILD_NOTES.md`, `README.md`, `STORE_LISTING.md`, and this file
   - There is **no** workflow file and **no signing material** in the zip — `build.yml` is unchanged and your keys stay in repo secrets only.
3. **Commit changes.** The build starts by itself.

---

## Section 2 — Build and read the signing line (same as before)

1. **Actions** tab → open the finished **Build APK** run.
2. On the run page (job summary / "Report build sizes and signing mode" step) it must say:
   - **`SIGNING: FIXED KEY`** ✅ — anything else, stop and tell Bullet. A fallback build cannot install over your current app.
3. The same step prints `app-release.apk -> N bytes` and `app-release.aab -> N bytes`. **Both must be under 5,000,000.** Tell Bullet both numbers.
4. Green tick → **Artifacts → SellerChat-apks** → download → unzip → use **app-release.apk**.

---

## Section 3 — Install OVER your current version (do NOT uninstall)

Do it on **Phone 1**, then **Phone 2** — over v2.2 (or v2.1 / v2.0; the seed passes chain in order by themselves):

a. On the phone, open SellerChat → Recents → confirm your test customer is there. That's the data worth protecting.
b. Install the v2.3 **app-release.apk** straight over it — **no uninstall**.
c. Success = two things, both: **it installs** AND **your test Recent is still there**, with its status/memo intact.
d. While you're there: open **Templates** — the 12 new French / Spanish / Arabic / Chinese templates should have appeared by themselves, exactly once each.

---

## Section 4 — v2.3 phone checks (detection + currency + new languages)

1. **Pre-detected settings:** open **Settings → Region & Currency**. Country, Currency and Preferred template language are already filled in — detected from the phone (SIM country first, then device language/region). On your Pakistani phones expect **Pakistan / PKR (Rs)** and a sensible language.
2. **Manual beats detection — forever:** change the **Currency** to **USD ($)** → leave Settings → force-stop the app → reopen → Settings still shows **USD**. (Detection ran once; it never comes back.) Change it back to PKR when you're done.
3. **Currency reaches the money screens:** with the currency still on USD, go to **Recents** → tap a customer → **Mark Paid** — the amount box starts with **$**, not Rs. The **This Week** total at the top of Recents uses **$** too. Cancel out; no fake payment needed.
4. **New languages in Templates:** the filter row now ends **French · Spanish · العربية · 中文**. Tap **العربية**: 3 Arabic templates, text **right-aligned, reading right-to-left**, in the normal phone font (NOT the Urdu Nastaliq font). Tap **中文**: 3 Chinese templates, left-to-right. French and Spanish: 3 each.
5. **v2.2 still intact:** Home → label just `Ayesha` → tap **Order confirm (Roman)** → the preview reads "…aap ka order confirm ho gaya hai. Total amount confirm karne ke liye reply karein." — no "ka ka", no brackets, no lone "Rs .".
6. **Fresh-install check (optional, one phone only):** if a phone has nothing worth keeping: uninstall SellerChat, install the v2.3 APK fresh, open it — First Run already shows your detected country, and Region & Currency is pre-filled. Skip this on any phone whose data matters; checks 1–5 are the real proof.

## Later (not now)

Real AdMob IDs and the $25 Play Console are still TODO(FAHAD) in the README. The French / Spanish / Arabic / Chinese templates — like the Tamil / Telugu / Marathi ones — are **AI drafts pending a native-speaker once-over** before a public launch.

---
---

# SellerChat — v2.2 Update Steps (kept for reference)

Fahad — v2.2 is the launch-polish build: two text bugs fixed (the "ka ka" in the order confirmation, and raw `[…]` brackets leaking into payment messages) plus one small feature, **Forward to Teammate**. Nothing about signing changes — your permanent key from v2.1 does all the work, and **this build replaces the planned v2.1.1 bump**: v2.2 IS the install-over proof now.

> ⚠️ **One thing to know before you judge it:** the Urdu order-confirmation template had the *same* "ka ka" construction as the Roman Urdu one, so it got the *same* fix (its `{item}` slot was removed too). If you want the Urdu one left as it was, tell Bullet — it is a one-line revert. The Roman Urdu fix stands either way.

---

## Section 1 — Upload the v2.2 files

1. Unzip **SellerChat-v2.2-updated-files.zip**.
2. In the SellerChat repo, **Add file → Upload files** and drag in everything from the unzipped folder. Paths are repo-relative, so files land on top of their old versions. You should see **11 files**:
   - `app/build.gradle.kts` (version 2.2 / code 6)
   - `app/src/main/java/com/fahad1005/sellerchat/data/SeedData.kt` (corrected templates + the correction list)
   - `app/src/main/java/com/fahad1005/sellerchat/data/SettingsStore.kt` (teammates + the one-time correction flag)
   - `app/src/main/java/com/fahad1005/sellerchat/viewmodel/MainViewModel.kt` (hardened message cleanup + forward action + correction pass)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/SettingsScreen.kt` (Team Members section)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt` (Forward to Teammate in the sheet)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/MainScreen.kt` (the sheet's Open Settings jump)
   - `BUILD_NOTES.md`, `README.md`, `STORE_LISTING.md`, and this file
   - There is **no** workflow file this time — `build.yml` is unchanged since v2.1 and already prints the SIGNING line.
3. **Commit changes.** The build starts by itself.

---

## Section 2 — Build and read the signing line (same as v2.1)

1. **Actions** tab → open the finished **Build APK** run.
2. On the run page (job summary / "Report build sizes and signing mode" step) it must say:
   - **`SIGNING: FIXED KEY`** ✅ — anything else, stop and tell Bullet.
3. The same step prints `app-release.apk -> N bytes` and `app-release.aab -> N bytes`. **Both must be under 5,000,000.** Tell Bullet both numbers.
4. Green tick → **Artifacts → SellerChat-apks** → download → unzip → use **app-release.apk**.

---

## Section 3 — Install OVER v2.1 (the proof — do NOT uninstall)

This is the test the v2.1.1 bump was invented for, now run by v2.2 itself. Do it on **Phone 1**, then **Phone 2**:

a. On the phone, open SellerChat (v2.1) → Recents → confirm your test customer from the v2.1 round is there. That's the data worth protecting.
b. Install the v2.2 **app-release.apk** straight over it — **no uninstall**.
c. Success = two things, both: **it installs** AND **your test Recent is still there**, with its status/memo intact.
d. Bonus check while you're in Recents: open **Templates** — your order-confirmation and payment templates should already show the corrected wording (the app rewrote the untouched defaults by itself; anything you had edited by hand is left exactly as you left it).

---

## Section 4 — v2.2 phone checks (the fixes + the feature)

1. **"ka ka" is gone:** Home → type a label that is just a name (e.g. `Ayesha`) → tap the **Order confirm (Roman)** chip → the preview must read "…aap ka order confirm ho gaya hai. Total amount confirm karne ke liye reply karein." — no "ka ka", no dangling "Rs". Tap **Open chat** and read the draft in WhatsApp: identical.
2. **No raw brackets:** tap the **Payment details** chip (any language) → the preview/draft must contain **no `[` or `]` and no `{` or `}`** anywhere.
3. **English order confirmation:** tap **Order confirmation** with no price anywhere → the draft's total sentence reads "I'll confirm the total amount on reply." — never "Total: Rs .".
4. **Forward to Teammate:** Settings → **Team Members** → add yourself or a second phone (name + number) → back in **Recents**, tap a customer → the sheet now ends with **Forward to Teammate** → tap it → tap the teammate's name → WhatsApp opens **on the teammate's number** with the message already typed: **"Customer: \<name or number\>. Please take over this chat."** Check Recents afterwards: forwarding did **not** add any new row.
5. **Spot-check nothing regressed:** banner still pinned at the bottom of Home; keyboard still hides the bottom navigation while typing; the notification permission is still asked ONLY on "Follow-up in 2 days".

## Later (not now)

Real AdMob IDs and the $25 Play Console are still TODO(FAHAD) in the README. **Play App Signing note:** when you publish, Google Play re-signs the AAB with the Play app key — your permanent key becomes your *upload* key. That's normal, and it's why it must stay yours. The Tamil / Telugu / Marathi seed templates are still AI drafts pending a native-speaker once-over.
