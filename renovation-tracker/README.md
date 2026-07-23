# Reno Tracker

A small, **fully offline** Android app for tracking a home renovation project.
No accounts, no server, no subscription — all your data lives in a local SQLite
database on your phone. Export to CSV and back up the whole database whenever you like.

## What it tracks

Everything is organized under **projects** (e.g. "Kitchen Remodel"). Each project has:

- **Paint & colors** — room/surface, brand, color name, paint code, sheen, hex, notes
- **Vendors** — name, contact, phone, email, website, notes
- **Equipment & materials** — item name, item/model number, vendor, unit cost, quantity (line total is computed)
- **Labor** — description, contractor/vendor, hours × rate *or* a flat cost (line total is computed)
- **Cost summary** — materials total, labor total, and grand total

## Export & backup

From the project list (⋮ menu):

- **Export all to CSV** — one CSV with every table as a labelled section; opens in Excel / Google Sheets
- **Backup database** — saves the entire SQLite database as a single `.db` file you can store anywhere (Drive, USB, etc.)
- **Restore database** — replaces the current data from a `.db` backup

From inside a project (⋮ menu) you can **export just that project** to CSV.

CSV and backup files are written wherever you choose via Android's file picker, so
they're easy to move off the device.

---

## Getting it onto your phone

### Option A — automatic APK build (no tools needed)

A GitHub Actions workflow (`.github/workflows/android-build.yml`) builds an
installable APK on every push.

1. Push this branch to GitHub.
2. Open the repo's **Actions** tab → the latest **Build Reno Tracker APK** run.
3. Download the **`reno-tracker-debug-apk`** artifact (a zip containing `app-debug.apk`).
4. Copy the APK to your phone and tap it to install. You may need to allow
   "Install unknown apps" for your file manager / browser the first time.

> This is a *debug*-signed build — perfect for personal use and sideloading.
> It is not meant for the Google Play Store.

### Option B — Android Studio

1. Open the `renovation-tracker/` folder in [Android Studio](https://developer.android.com/studio) (free).
2. Let it sync Gradle, then press **Run ▶** with your phone connected (USB debugging on) or an emulator selected.

Both options are completely free. Only publishing to the Play Store would cost
anything (a one-time $25 developer fee) — and you don't need that to use the app.

## Tech

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) for local storage
- Navigation Compose
- minSdk 26 (Android 8.0+), targetSdk 35
