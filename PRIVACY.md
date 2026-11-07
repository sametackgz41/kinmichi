# Privacy

Short version: Kinmichi keeps your data on your phone. It has no ads, no analytics and no
tracking. The only time anything leaves the phone is when **you** turn on Google sync, share a
card, or report a problem.

## What stays on your phone

Habits, check-ins, notes, moods, goals, plans, focus sessions and settings are stored in a local
database on the device. Backups you export (CSV or JSON) are files you choose where to save.

The app lock PIN is stored as a salted hash, never as the PIN itself.

## Google sign-in and cloud sync (optional, off until you sign in)

If you sign in with Google, a copy of your Kinmichi data is saved to Google Cloud Firestore in the
project that runs Kinmichi's sync, under your account only. Security rules let each signed-in user
read and write their own data and nobody else's. The data is **not end-to-end encrypted**: it is
protected by Google's storage encryption and those rules, which means the project's
administrator could technically access it. If that matters to you, don't turn on sync and use
the local backup instead.

You can delete the cloud copy and your account at any time from **Settings › Google account**.

## Health Connect (optional, read-only)

When you link a habit to steps, distance, calories, exercise or sleep, Kinmichi asks Health
Connect for permission to **read** that one kind of data. It reads today's total (for sleep, last
night's) and stores only that number as the habit's progress. It never writes to Health Connect
and never sends health data anywhere. You can withdraw the permission in Health Connect's settings.

## Wear OS

With the watch app installed, today's habit list is sent from the phone to your paired watch over
the Wear OS Data Layer, and taps on the watch are sent back. It stays between your two devices.

## Network

Kinmichi only uses the internet for Google sign-in and sync, and to check GitHub for a newer
version (a single request to `api.github.com` for the latest release; it sends no personal data).
The update check can be turned off in Settings.

## Reporting a problem

**Report on GitHub** opens a pre-filled issue in your browser with the app version, Android
version, phone model and the last error log. Nothing is sent until you review it and press
Submit on GitHub, and the issue is public. Remove anything you don't want to share.

## Questions

Open an issue: <https://github.com/sametackgz41/tsuzuku/issues>
