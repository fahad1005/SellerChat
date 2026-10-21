# SellerChat v1.0.2 — Build #2 fix (read this first)

## What went wrong in Build #2 (again Bullet's code, not your upload)
Your icon fix uploaded perfectly — Build #2 died on the NEXT thing:
`combinedClickable` (the tap + long-press handler on the chips and the
Recents rows) is an experimental Compose API, and the three functions
using it were missing the `@OptIn` permission line the compiler demands.
Fix: added the OptIn line + import to exactly those 3 functions.
No behaviour changes. Version bumped to 1.0.2 (versionCode 3).

## Changed files in this zip (3)
- `app/build.gradle.kts` — version bump only
- `app/src/main/java/com/fahad1005/sellerchat/ui/HomeScreen.kt`
- `app/src/main/java/com/fahad1005/sellerchat/ui/RecentsScreen.kt`

## Your upload steps (same as always)
1. Unzip this file on your PC.
2. In the SellerChat repo: **Add file → Upload files**.
3. Drag in the **inside contents** (the `app` folder + this file).
   Check staged paths start with `app/…`.
4. **Commit changes.**
5. **Actions** tab → **Build #3** starts by itself (~4 minutes).
6. Green tick → open the run → **Artifacts → SellerChat-apks** → download →
   install **app-debug.apk** and run the phone test list.
7. Tell Bullet the release size from the **"Report APK sizes"** step
   (budget: under 5,000,000 bytes).
8. Red X → screenshot the red step's error lines for Bullet.
