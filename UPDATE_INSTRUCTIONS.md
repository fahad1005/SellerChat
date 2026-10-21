# SellerChat — v2.0 Update Steps (read this first)

Fahad — this is an **update to the existing SellerChat repo** (github.com/fahad1005/SellerChat), not a new app. Same routine as the v1.0.1 / v1.0.2 fixes: upload the changed files, let the robot build, install **over** your current app.

## Step 1 — Upload the changed files

1. Unzip **SellerChat-v2.0-updated-files.zip** on your PC.
2. In the SellerChat repo, use **Add file → Upload files** and drag in everything from the unzipped folder. The paths inside the zip are already repo-relative (`app/…`, `gradle/…`, plus the `.md` docs at the root) — dropping them at the repo root overwrites the old versions in place.
3. Quick sanity check on the staged list: you should see 27 files, including `app/src/main/res/font/noto_nastaliq_urdu.ttf` (the new Urdu font) and `app/src/main/java/com/fahad1005/sellerchat/notify/FollowUp.kt` (the new reminder code). No `.github` changes this time — the build robot is unchanged.
4. **Commit changes.**

## Step 2 — Watch the build

1. Open the **Actions** tab — the build starts by itself.
2. Green tick = success. Red X = open the run, find the red step, screenshot the error lines for Bullet (same drill as always).

## Step 3 — Download + install OVER v1.0 (do NOT uninstall)

1. Open the green run → **Artifacts → SellerChat-apks** → download → unzip.
2. Install **app-debug.apk** on your phone **on top of the existing SellerChat** — tap the file and choose Update/Install. ⚠️ **Do not uninstall first**: v2's whole point is that your v1 data survives, and uninstalling would delete the very data we're checking.
3. **Signing caveat (same as v1):** CI signs these builds with a throwaway key made on the runner, only so the APKs are installable and measurable. If Android ever refuses the install with a signature error, **stop and tell Bullet — do not uninstall to force it.** The data-survival check below only means something with an install-over.

## Step 4 — Data-survival check (the important one)

1. Open SellerChat after the update.
2. **Recents:** your v1 customers must all still be there, with their labels. (They'll show a fresh "Inquiry" chip and a waiting clock counting from when you last messaged them — that's expected.)
3. **Templates:** your v1 templates must all still be there, **plus 12 new ones** at the bottom of the list (Hindi, Tamil, Telugu, Marathi price/payment/delivery templates).
4. **Settings:** your country / default app / your number must be as you left them.
5. Tell Bullet the **release APK size** from the run's "Report APK sizes" step. Budget: **under 5,000,000 bytes.** (v1.0 measured 3.3 MB; v2 adds WorkManager + the Urdu font, so somewhat bigger is expected — over budget is not.)

## Step 5 — New v2 acceptance checks (phone)

1. **Try Demo:** force-stop and re-open the app fresh (or reinstall the debug APK on a second phone) → on the first screen tap **Try Demo** → WhatsApp opens with the price reply typed for +92 300 1234567. Nothing is sent; no new Recent appears afterwards.
2. **One-time tip:** on Home, a banner says "Paste a number → Pick a template → Open Chat." Dismiss it, close and reopen the app — it must never come back.
3. **Home order:** number → WhatsApp chooser → template chips → label → Open Chat button. The ad is still only at the bottom of Home.
4. **Status chip:** in Recents, tap a customer's chip — Inquiry → Ordered. Tap again → the **amount prompt** appears (your currency symbol is shown); enter an amount and Save → chip says Paid. Do it again later with **Skip** → still Paid, no amount. Tap again → Shipped, once more → Inquiry.
5. **Waiting clock:** an Inquiry customer shows "Waiting …" under their name (orange now; it turns red after 24h).
6. **Memo:** tap a customer row → **Add Memo** → type "Size 14, blue" → Save → the memo shows pinned under their name in the list.
7. **Follow-up + the permission rule:** tap a customer row → **Follow-up in 2 days** → the notification permission popup appears **now, and only now** (it must NOT have appeared at app launch or first run). Allow it → the row shows "Reminder set". (Deny it on a second customer → the row shows "Reminder set (in-app)" — the reminder still saved.)
8. **Urdu in Nastaliq:** Templates tab → اردو filter → the templates render in the flowing Nastaliq font, right-to-left, nothing clipped. Open one in the editor → it edits in the same font.
9. **Pulse row:** top of Recents reads like `Leads: N | Waiting: N | Paid: N | This Week: Rs <sum>` — the Paid and This Week numbers must match the customers you marked paid in step 4.
10. **Search fix:** in Recents, search **order** — the customer who used the "Order confirm" template must now appear (v1 missed this).
11. **Editor fix:** open any template → the {name} {price} {item} {qty} buttons — all four fully visible (scroll the row sideways if needed); {qty} must not be a clipped blank pill anymore.
12. **Core flow unchanged:** paste a number → Open Chat → template typed, not sent. Airplane mode → everything works except the ad.

## Later (not now)

Real AdMob IDs, your own release keystore / Play App Signing, the $25 Play Console, and the hosted privacy-policy URL are still **TODO(FAHAD)** in the README — launch-time steps, one at a time. The Tamil / Telugu / Marathi seed templates are AI drafts — worth a native-speaker once-over before any public release (noted in BUILD_NOTES).
