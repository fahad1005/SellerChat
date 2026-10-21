# SellerChat v1.0.1 — Build #1 fix (read this first)

## What went wrong in Build #1 (Bullet's code, not your upload)
Three icons used in the code — Paste (Home), History + Description (bottom nav) —
are NOT in the small `material-icons-core` library the app uses on purpose
(the full icon library is several MB and would break the under-5 MB budget).
Fix: those 3 icons now ship as tiny vector files (same trick the app already
uses for its QR icon). No feature changes. Version bumped to 1.0.1 (versionCode 2).

## Changed files in this zip (6)
- `app/build.gradle.kts` — version bump only
- `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt`
- `app/src/main/java/com/fahad1005/sellerchat/ui/MainScreen.kt`
- `app/src/main/res/drawable/ic_content_paste.xml` — NEW
- `app/src/main/res/drawable/ic_history.xml` — NEW
- `app/src/main/res/drawable/ic_description.xml` — NEW

## Your upload steps (same as always)
1. Unzip this file on your PC.
2. In the SellerChat repo: **Add file → Upload files**.
3. Drag in the **inside contents** of the unzipped folder (the `app` folder +
   this file). Check staged paths start with `app/…` — NOT with the zip folder name.
4. **Commit changes.** GitHub overwrites the 3 changed files and adds the 3 new ones.
5. **Actions** tab → **Build #2** starts by itself. Give it ~4 minutes.
6. Green tick → open the run → **Artifacts → SellerChat-apks** → download →
   install **app-debug.apk** and run the phone test list.
7. Tell Bullet the release size from the **"Report APK sizes"** step
   (budget: under 5,000,000 bytes).
8. Red X → screenshot the red step's error lines for Bullet.
