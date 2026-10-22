# SellerChat — v2.1 Update Steps (read this first)

Fahad — v2.1 is the polish + signing release. The road is short now: **upload the files → paste your key into the secrets → build once more.** Bullet made the permanent key for you and sent it in chat (a password + one text file) — there is no key-making ceremony anymore. Do the sections in order, and every build from then on signs with YOUR permanent key: updates install on top of each other and your data survives, on every phone, forever.

---

## Section 1 — Upload the v2.1 files

1. Unzip **SellerChat-v2.1-updated-files.zip**.
2. In the SellerChat repo, **Add file → Upload files** and drag in everything from the unzipped folder. Paths are repo-relative, so files land on top of their old versions. You should see **11 files**, including:
   - `.github/workflows/build.yml` (updated robot — signs with your key, also builds the AAB, and prints the SIGNING line on every run)
   - `app/build.gradle.kts` (version 2.1 + the permanent signing config)
   - `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt`, `MainScreen.kt`, `RecentsScreen.kt` (the UI fixes)
   - `BUILD_NOTES.md`, `README.md`, `STORE_LISTING.md`, and this file
   - ⚠️ If the upload silently skips the `.github` folder (it has bitten us before), do the trick you already know from v1: **Add file → Create new file**, type the exact path `.github/workflows/build.yml`, paste in the contents of the **SellerChat-build-workflow-v2.1.txt** file Bullet sent in chat, and commit.
3. **Commit changes.**
4. A build will start by itself, and its run page will say `SIGNING: THROWAWAY FALLBACK (secrets not set)`. **Ignore that build completely** — your key isn't in yet. Sections 2–3 fix that, then you build one more time.

---

## Section 2 — One paste: put your permanent key in the secrets

Bullet made the key for you. In chat you received two things: **a password**, and a file called **SellerChat-signing-key-BASE64.txt** (that long text IS your key, written out as letters).

1. Open **SellerChat-signing-key-BASE64.txt**, select **ALL** the text, and copy it.
   - ⚠️ **NEVER upload this file to the repo.** It goes in exactly ONE place: the secret in step 2. Keep your own copy of it somewhere safe that is NOT this repo (a notes app, a USB stick) — anyone who has this text plus your password owns your app's identity.
2. Go to the repo's **Settings → Secrets and variables → Actions → New repository secret** and create these THREE secrets, one at a time:
   - Name: `SIGNING_KEYSTORE_B64` → Secret: paste the copied text
   - Name: `SIGNING_STORE_PASSWORD` → Secret: the password Bullet gave you in chat
   - Name: `SIGNING_KEY_PASSWORD` → Secret: the SAME password again (store and key password are the same, by design)
3. **Write the password in your notes / password manager NOW.** If you lose this password, you can never update the app under the same identity again — you would have to start over as a "new" app. This is the one thing in the whole project where losing something is forever.

**One consequence to know now:** the SellerChat on your phone today (v2.0) was signed with a throwaway key, so **v2.1 needs ONE last uninstall** before it will install. That is expected, and it is the LAST time — from v2.1 onward every update installs on top and your data survives. The test protocol in Section 4 is built around exactly this.

---

## Section 3 — Build once more (and read the signing line)

1. **Actions** tab → open the **Build APK** workflow → **Run workflow** (or open the build that fired in Section 1 → **Re-run jobs**).
2. Open the run. At the top of the run page (job summary) and in the **"Report build sizes and signing mode"** step you must see:
   - **`SIGNING: FIXED KEY`** ✅ — this is the only acceptable result.
   - If you see `SIGNING: THROWAWAY FALLBACK (secrets not set)` — STOP. A secret is missing or misnamed. Recheck the three names in Section 2 step 2 character by character, then **Re-run jobs**. Do not use a fallback build for the phone test.
3. The same step prints the sizes: `app-release.apk -> N bytes` and `app-release.aab -> N bytes`. **Both must be under 5,000,000.** Tell Bullet both numbers.
4. Green tick → **Artifacts → SellerChat-apks** → download → unzip. You'll use **app-release.apk** for the phone test.

---

## Section 4 — Multi-device test protocol (the installation proof)

Do this on **Phone 1**, then repeat on **Phone 2**. The point: prove that a FIXED KEY build updates cleanly and keeps data.

**On each phone:**

a. **Uninstall any old SellerChat.** (LAST time ever — the old builds were throwaway-signed; nothing signed that way can be updated, by Android's design.)
b. First, confirm on the run page that the build said **`SIGNING: FIXED KEY`** (Section 3). Then install the **v2.1 app-release.apk**.
c. Inside v2.1, **create one Recent**: paste any number, pick a template, **Open chat** (you don't have to send anything in WhatsApp — just come back). Check the customer now shows in Recents. This is the data worth protecting.
d. **When the follow-up bump build arrives (v2.1.1 — Bullet will send it; it's a version-number-only build with the same key):** install its app-release.apk **OVER v2.1 — do NOT uninstall.** Success = two things, both: **it installs** AND **your Recent from step c is still there.**
e. Repeat a–d on the second phone.

A build is only "the fix" if its run page shows **FIXED KEY**. A fallback build must NOT be used for this test — if any run shows the fallback line, tell Bullet before installing anything from it.

---

## Section 5 — v2.1 phone checks (the six fixes)

1. **Keyboard (fix 1):** on Home, tap the number field so the keyboard opens → the bottom navigation (Home / Recents / Templates) must be **gone** while you type (not floating over the keyboard), and you can **scroll down to the Open chat button** with the keyboard still open. Close the keyboard → the navigation comes back.
2. **Banner (fix 2):** Home shows the test ad banner pinned at the very bottom of the screen, above the navigation bar — visible without scrolling.
3. **Preview + auto-fill (fix 3):** type the label **`Ayesha — kurta`**, tap the **Price inquiry reply** chip → directly under the chips, a preview shows the message with **Ayesha** and **kurta** filled in and **no `{ }` brackets anywhere**. Tap **Open chat** → the message in WhatsApp is exactly the preview. (Also try a label with no dash — just a name — the `{item}` spot must come out blank, still with no brackets.)
4. **Chip order (fix 4):** with a Pakistani (+92) country set and no Preferred Language chosen in Settings, the first chips are English / Urdu / Roman Urdu templates — not Tamil/Telugu. Scroll the chip row to the very end: **"No template" is the last chip.**
5. **Recents card (fix 5):** each customer row shows a small colored **dot + thin badge** (Inquiry = orange, Ordered = blue, Paid = green, Shipped = purple) next to the name, and the template name as small plain text under the number line — no more stacked pills on the right. Tap the badge → status cycles. Tap the row → the sheet offers **Open Chat / Follow-up in 2 days / Add Memo / Mark Paid**.
6. **Still true from v2 (spot-check):** the notification permission is asked ONLY when you tap "Follow-up in 2 days"; Urdu templates still render in Nastaliq (eyeball an Urdu chip title on Home — it sits taller; it must not be cut off); everything except the ad works in airplane mode.

## Later (not now)

Real AdMob IDs and the $25 Play Console are still TODO(FAHAD) in the README. **Play App Signing note:** when you eventually publish, Google Play will re-sign the AAB with the Play app key — your permanent key becomes your *upload* key. That's normal and it's exactly why it must be yours and permanent. The Tamil / Telugu / Marathi seed templates are still AI drafts pending a native-speaker once-over.
