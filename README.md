# Fuelbook — native Android fuel tracker

A minimal, offline, single-activity Android app written in Java with the Android SDK. No advertising, accounts, internet permission, or third-party runtime libraries.

## Features
- Create, select, rename and delete multiple vehicles.
- Save fuel cost (₹), quantity in litres, odometer reading in km and an optional estimated mileage.
- Automatically calculate actual mileage after the second fill-up for each vehicle.
- Summary of latest mileage, distance and difference from estimated mileage; readable history for every vehicle.
- Local SQLite persistence, with confirmation before deleting records.
- One-file CSV export/import using Android's system file picker. Supports multiple vehicles, quoted vehicle names and deduplication on vehicle + odometer.
- Light and dark themes, saved between launches.

## Mileage calculation
For an entry after the first: **(current odometer − previous odometer) / litres added at the current fill-up**, in km/L. This is accurate only for the **full-tank-to-full-tank** method; partial fills, a different fill level, or missing entries affect accuracy. The first entry has no actual-mileage result. Entries are ordered by odometer per vehicle, including imported historical records.

## Open and build
1. Install Android Studio (with Android SDK Platform 35, Build Tools, and JDK 17).
2. Open the `Fuelbook` directory as a project.
3. Sync Gradle using Android Gradle Plugin 8.7.3 and Gradle 8.9. (If the IDE prompts for a Gradle distribution, select/install Gradle 8.9; this source archive does not bundle Gradle binaries.)
4. Run the `app` configuration on an Android device/emulator (Android 6.0+), or select **Build > Build APK(s)**.

The source has not been compiled in this environment, which does not have an Android SDK or Gradle distribution installed. No APK is included.

## CSV schema

`type,vehicle,estimated_km_per_l,epoch_millis,odometer_km,liters,amount`

`vehicle` rows define vehicle profiles; `fill` rows define fill-ups. `epoch_millis` is Unix time in milliseconds; amounts use ₹ for display only and are stored as plain decimal numbers in CSV. CSV import is transactional (bad rows roll back the whole import). Import merges vehicles case-insensitively, skips duplicate vehicle/odometer fill-ups, and updates profile estimates from vehicle rows.

## Privacy
All entries and theme preferences live on your device. Import/export uses the system document picker, so no storage permissions or network access are required. Device backup may include local application data according to Android backup settings.
