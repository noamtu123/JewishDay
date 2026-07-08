# JewishDay

A modern Jewish companion app — free and open source.

JewishDay shows the Hebrew date, the day's zmanim, and your daily learning, and can keep
today's Hebrew date on your phone at all times. It's built to be the kind of app I wish every
app was: fully free and open source.

## Features

- Hebrew date, parsha, Yom Tov, fast days, the Omer, and Rosh Chodesh
- Location-based zmanim, with a configurable calculation method for every time
- Shabbat and fast times — candle lighting, Motzei Shabbat, and Rabbeinu Tam
- Daily learning (Daf Yomi, the daily Rambam, and more)
- A compass that points toward Jerusalem for prayer
- Full Hebrew and English interfaces, with light and dark themes

**Always-on Hebrew date.** An optional notification puts today's Hebrew day in your status bar
and quietly rolls it over each day, so the date is always one glance away — no need to open the app.

## Free and open source

JewishDay is fully FOSS, released under the GNU GPL v3.0. The complete source is here for anyone
to read, build, and improve. That openness is the point — it's the app I wish every app was.

Built collaboratively with an AI coding assistant, then reviewed and shaped by me. Worth saying
in the same spirit of openness: every release still goes through tests, lint, and on-device use.

## Built with

- Kotlin and Jetpack Compose
- Material 3
- Hilt for dependency injection, DataStore for settings
- [KosherJava](https://github.com/KosherJava/zmanim) for the Hebrew calendar and zmanim
- The Hebcal API for daily-learning schedules
- A foreground service that keeps the always-on Hebrew-date notification current
- Gradle build; single release variant, tested and linted on each build

## Building

Requires JDK 17 and the Android SDK.

```sh
./gradlew :app:assembleRelease
```

Release builds are signed with a local keystore kept out of the repository; debug builds work
without it.

## License

Copyright (C) 2026 noamtu123

Licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.
