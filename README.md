# JewishDay

Hebrew dates, zmanim, and daily learning in one quiet, private Android app.

JewishDay shows today's Hebrew date and the day's halachic times, follows your
daily-learning schedule, and points you toward Jerusalem for prayer — with no ads,
no tracking, and no accounts. It's meant to be the kind of app I wish every app was.

## Features

- **Hebrew date at a glance** — date, parsha, Yom Tov, fast days, the Omer count, and
  Rosh Chodesh. The displayed date rolls over at sunset, the way the Jewish day actually works.
- **Zmanim for where you are** — location-based halachic times with a configurable method
  for every zman (degrees, fixed minutes, zmaniyot, and special opinions), so the times can
  follow your minhag rather than a single fixed calculation.
- **Shabbat & fast times** — candle lighting, Motzei Shabbat, Rabbeinu Tam, and the start and
  end of the currently active fast.
- **Daily learning** — Daf Yomi, the daily Rambam, and other study schedules.
- **Prayer compass** — a magnetic-declination-corrected compass pointing toward the
  Kodesh HaKodashim in Jerusalem.
- **Hebrew-date notification** — an optional persistent status-bar icon showing today's
  Hebrew day.
- **Bilingual and themeable** — full Hebrew and English interfaces, with several light and
  dark themes, including a true-black AMOLED theme.

## Free, open, and private

This is the part that matters most, so it isn't buried at the bottom:

JewishDay is **free and open source**. **No ads. No analytics. No tracking. No accounts.**
It asks for your location only to compute zmanim and the prayer compass, and nothing about
your day ever leaves your device. The entire source lives in this repository for anyone to
read, audit, or build for themselves. That transparency is the point of the project, not a
footnote to it.

## How it's built

- Kotlin, Jetpack Compose, and Material 3
- Hilt for dependency injection; DataStore for settings
- [KosherJava](https://github.com/KosherJava/zmanim) for the Hebrew calendar and zmanim math
- The Hebcal API for daily-learning schedules
- A foreground service that keeps the optional Hebrew-date notification current across the day

And, in the same spirit of transparency: this app is largely **vibe coded** — built
side by side with an AI coding assistant, then guided, reviewed, and shaped by me. It's worth
saying plainly, because if the source is open for you to read, how it was written should be
open too. It's held to the same bar as anything written by hand: unit tests, lint, and
on-device use before every release.

## Building

Requires JDK 17 and the Android SDK.

```sh
./gradlew :app:assembleRelease
```

Release builds are signed with a local keystore that is intentionally kept out of the
repository; debug builds work without it.

## License

Copyright (C) 2026 noamtu123

This project is licensed under the GNU General Public License v3.0.
See the [LICENSE](LICENSE) file for details.
