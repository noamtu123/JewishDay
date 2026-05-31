# JewishDay

A modern Android rebuild of the original JewishDay app.

The original app is kept in this repository as reference material under `legacy-reference/`. The active Android project is the new Compose implementation under `app/`.

## Modern Base

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin 8.13
- Kotlin 2.0 Compose compiler plugin
- KosherJava Zmanim 2.5.0
- KosherJava Hebrew calendar formatting

## Target Feature Set

- Daily Jewish date
- Persistent daily Jewish date notification
- Optional Hebrew and English date status-bar notification icons
- Zmanim by current or saved location
- Shabbat and holiday-related times
- Davening and day-time reminders
- Mizrach compass toward Jerusalem
- Hebrew/English app modes
- Saved places with offline known-place search
- AMOLED black and blue-white Israel themes

## Legacy Reference

The legacy Java app remains in `legacy-reference/` for reference only. It is not the active Gradle module.
