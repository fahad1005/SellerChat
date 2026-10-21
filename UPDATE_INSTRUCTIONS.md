# SellerChat — Your Upload Steps (read this first)

Fahad — this is a **brand-new app** (SellerChat), separate from StatusSaver. New app = new GitHub repo. Same robot-builder routine you already know.

## Step 1 — Create the new repo

1. Go to **github.com** → top-right **+** → **New repository**.
2. Name it exactly: **SellerChat**
3. Public or Private — your choice (Private is fine).
4. Do **NOT** tick "Add a README" (we are uploading our own files).
5. Click **Create repository**.

## Step 2 — Upload the project

1. Unzip **SellerChat-v1.0-project.zip** on your PC.
2. In the new repo, click **uploading an existing file**.
3. Drag in the **inside contents** of the unzipped folder — you should see these go in:
   - the **app** folder
   - the **.github** folder (⚠️ hidden folder — if you can't see it, turn on "Hidden items" in File Explorer View settings; the build will NOT run without it)
   - the **gradle** folder
   - `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `.gitignore`
   - the five `.md` files (README, PRIVACY_POLICY, STORE_LISTING, BUILD_NOTES, this one)
4. Check the staged list: paths must start with `app/…`, `.github/…`, `gradle/…` — **not** with a folder named `sellerchat/…` or `SellerChat-v1.0-project/…`. If they do, you dragged the outside folder — remove and drag the *inside* contents instead.
5. **Commit changes.**

## Step 3 — Watch Build #1

1. Open the **Actions** tab. "Build APK" starts by itself.
2. First build downloads the whole Android toolchain — give it several minutes.
3. Green tick = success. Red X = open the run, find the red step, and screenshot the error lines for Bullet (same drill as StatusSaver).

## Step 4 — Download + the two numbers I need

1. Open the green run → **Artifacts → SellerChat-apks** → download → unzip. Two files:
   - **app-debug.apk** → install this on your phone and test.
   - **app-release.apk** → the small, optimized build. (CI signs it with a throwaway key made on the runner, only so it is a real measurable APK. It is for the size test — the debug APK is the one to daily-test.)
2. Open the run's **"Report APK sizes"** step and tell me the release size it prints. Budget: **under 5,000,000 bytes.**

## Step 5 — Phone test (the acceptance list)

- Fresh install → one first-run screen → Skip or Get started both work.
- Paste a customer number → **Open chat** → WhatsApp opens with the template **typed in the box, not sent**.
- Same test with **WhatsApp Business** if the phone has it.
- Re-message the same number → Recents updates the same row (no duplicate).
- Templates tab: edit one Urdu template — text must sit correctly (right-to-left).
- Home → **Show QR** → QR appears; **Share to my status** → set your number → share works.
- Airplane mode ON → everything still works except the ad space stays empty.
- Ads check: one small banner at the bottom of Home only. **No full-screen ad, ever.**

## Later (not now)

Real AdMob IDs, your own release keystore / Play App Signing, the $25 Play Console, and the hosted privacy-policy URL are all marked **TODO(FAHAD)** in the README — we do those at launch time, one step at a time.
