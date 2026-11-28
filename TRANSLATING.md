# Translating JewishDay

JewishDay speaks English and Hebrew. Native speakers are welcome to improve either, or to add a
language of their own. You don't need to know Kotlin: a language is a folder of text files.

## How the app's text is organised

| What | Where |
|---|---|
| English, the source text | `app/src/main/res/values/strings.xml` |
| Hebrew | `app/src/main/res/values-iw/strings.xml` |
| Your language | `app/src/main/res/values-<code>/strings.xml` |

Every English string has a comment above it saying where it appears and what it means. Read it
before translating: "Shabbat ends" is a label for a time, "fast" follows a "+", and some strings
are halachic terms best left transliterated in your language.

A few things are not translated, on purpose. Strings marked `translatable="false"` are not text
(the app's name, a number format, the way two Rambam readings are joined), and the
home-screen widget picker's labels in `values/widget.xml` carry every language side by side,
because the launcher shows them in the phone's language rather than the app's.

## Improving a translation

Edit the string in your language's `strings.xml`. Keep:

- **Placeholders** exactly as they are: `%1$s`, `%2$d` and so on stand for a value the app fills
  in. You may move them within the sentence, but not drop, renumber or retype them.
- **Escapes**: write an apostrophe as `\'`, a double quote as `\"`, and a new line as `\n`.
- **Plurals**: a `<plurals>` entry has one line per grammatical number your language has
  (`one`, `two`, `few`, `many`, `other`; see the
  [CLDR plural rules](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html)).
  `other` is always needed.

## Adding a language

1. Create `app/src/main/res/values-<code>/strings.xml`. Use the language's two-letter code, `fr`
   for French or `ru` for Russian; for a regional variant, `values-pt-rBR`. (Hebrew's folder is
   `values-iw`, the old code every Android version still reads.)
2. Copy the `<string>` and `<plurals>` entries from `values/strings.xml`, leaving out those marked
   `translatable="false"`, and translate them. Leave the comments behind: they live in the English
   file.
3. Build. That is all it takes for the language to show up in the app's own language picker and
   under Android 13's *Settings → Apps → JewishDay → Language*; both lists are generated from the
   translation folders. Lint fails the build if a string is missing, and a test fails if a
   placeholder was changed.

### The calendar's own names

Holiday, month, parsha and tractate names, the Hebrew date, and the daily-learning references
don't come from `strings.xml`. They are worked out by the calendar library, and each language
writes them its own way. A new language gets the English transliteration ("Rosh Hashana",
"Parashat Bereshit", "5 Tishrei, 5787") until it has a formatter of its own.

Writing one means implementing `LanguageFormatter` in
`app/src/main/java/com/noamtu/jewishday/i18n/` (see `EnglishLanguageFormatter` and
`HebrewLanguageFormatter`) and returning it from `LanguageFormatter.forLocale`. That part does need
some Kotlin. Open an issue if you'd like help with it.

### Release notes

The update dialog shows each release's notes in the app's language when they have it. They are
written in English on the GitHub release page, and each translation goes in an HTML comment
named by its language code:

```markdown
### New
- Faster start

<!-- lang:fr
### Nouveautés
- Démarrage plus rapide
-->
```

## Checking your work

```sh
./gradlew :app:testReleaseUnitTest :app:lintRelease
```

Then switch the language on a phone, in the app's Settings or in the system settings, and walk
through the screens, the widgets and the status-bar date.
