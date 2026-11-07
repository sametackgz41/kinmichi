# Contributing to Kinmichi

Thanks for helping. Kinmichi is a small project, so the easiest contributions are the most useful
ones: telling us what broke, suggesting what would help you keep going, and translating.

## Reporting a bug

Use **[New issue › Bug report](https://github.com/sametackgz41/tsuzuku/issues/new?template=bug_report.yml)**.
The fastest way is from the app: **Settings › Error reports › Report on GitHub**. It fills in
the version, Android version, phone model and the last error log for you.

Please include what you did, what you expected and what happened. A screenshot or a short screen
recording helps a lot.

## Suggesting a feature

Use **[New issue › Feature idea](https://github.com/sametackgz41/tsuzuku/issues/new?template=feature_request.yml)**.
Describe the problem you want solved before the solution you have in mind; there may be a
simpler way that fits the app.

## Translating

All text lives in Android string files:

- English (the reference): `app/src/main/res/values/strings.xml`
- Turkish: `app/src/main/res/values-tr/strings.xml`
- The watch app: `wear/src/main/res/values/strings.xml`

To add a language:

1. Copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-XX/strings.xml`, where
   `XX` is the [language code](https://developer.android.com/guide/topics/resources/providing-resources#LocaleQualifier)
   (for example `es`, `de`, `pt-rBR`, `ja`).
2. Translate the text between the tags. Keep `%1$s`, `%2$d` and similar placeholders exactly as
   they are; you can move them within the sentence.
3. Escape apostrophes as `\'`.
4. Add the language to `LanguagePreference` in
   `app/src/main/java/app/tsuzuku/data/preferences/AppSettings.kt` so it appears in Settings.
5. Open a pull request. If you are not comfortable with Git, attach the file to an issue instead.

Partial translations are fine: missing strings fall back to English.

## Code

Requirements: JDK 17 and the Android SDK (compile SDK 36). The app builds without Firebase.

```bash
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleDebug          # debug APK
```

- Kotlin, Jetpack Compose, Material 3 Expressive, Room, Glance widgets.
- Keep logic in `model/` as plain Kotlin with unit tests; screens in `ui/` stay thin.
- Every user-visible string goes in both `values/` and `values-tr/`.
- Screen changes: run the preview gallery (see README) and attach before/after images to the PR.
- Database changes need a Room migration in `TsuzukuDatabase.kt`, and the field in the backup
  format in `BackupRepository.kt`.

Small, focused pull requests are easiest to review.

## Code of conduct

Be kind and assume good intent. Harassment of any kind is not welcome.
