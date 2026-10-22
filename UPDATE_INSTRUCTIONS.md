# SellerChat — v2.4 Update Steps (read this first)

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
