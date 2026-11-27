# Wording pass — every change

A review of every user-facing English and Hebrew string: `values/strings.xml`, `values-iw/strings.xml`
and the widget picker's bilingual labels in `values/widget.xml`.

The pass changes no keys, `storageValue`s, placeholders, plurals, halachic terms or opinion names.
The calendar's own names (holidays, months, parshiot, masechtot, Hebcal readings, all produced by
KosherJava/Hebcal through the `LanguageFormatter`s) were reviewed and left as they are.
`app/src/test/resources/golden-text.txt` was regenerated; its diff is the record of every
on-screen, widget, icon and TalkBack line this changes.

## English (`values/strings.xml`, `values/widget.xml`)

| Key | Old | New | Reason |
|---|---|---|---|
| `notification_channel_date_status_icon` | Date status icons | Hebrew date icon | There is one icon, and it shows the Hebrew date (the Hebrew name is already singular). |
| `notification_channel_date_status_icon_description` | Persistent Hebrew date icon | Keeps today's Hebrew date in the status bar | It no longer repeats the new channel name, and now says what the icon does. |
| `report_issue` | Report bug or request feature | Report a bug or request a feature | Adds the missing articles. |
| `zmanim_candle_prompt_title` | Welcome Shabbat | Welcoming Shabbat | A title should be a noun phrase (like the Hebrew "קבלת שבת"), not a command. |
| `zmanim_loading` | Calculating zmanim | Calculating zmanim… | Same ellipsis as the other "in progress" messages ("Waiting for current location…"). |
| `zmanim_location_jerusalem` | Times based on Jerusalem | Times for Jerusalem | Plainer English for "calculated for". |
| `zmanim_location_named` | Times based on %1$s | Times for %1$s | Same, so it matches the line above. |
| `holy_day_starts_first_day` | the first day of Yom Tov starts | First day of Yom Tov starts | Bug: it opens a line on the header card and on widget rows, so it showed in lowercase. Now in the form of "Yom Tov starts". |
| `holy_day_ends_first_day` | the first day of Yom Tov ends | First day of Yom Tov ends | Same. |
| `holy_day_starts_second_day` | the second day of Yom Tov starts | Second day of Yom Tov starts | Same. |
| `holy_day_ends_second_day` | the second day of Yom Tov ends | Second day of Yom Tov ends | Same. |
| `holy_day_starts_third_day` | the third day of Yom Tov starts | Third day of Yom Tov starts | Same. |
| `holy_day_ends_third_day` | the third day of Yom Tov ends | Third day of Yom Tov ends | Same. |
| `zman_chatzot_hayom` | Chatzot HaYom | Chatzot Hayom | Matches its neighbours in the zmanim list ("Alot Hashachar", "Plag Hamincha"). |
| `zman_chatzot_halaila` | Chatzot HaLaila | Chatzot Halaila | Same. |
| `method_6_minutes_after_alot` | 6 min after alot hashachar | 6 min after Alot Hashachar | Capitalised the way the zman name and the other methods ("Alot 16.1° to sunset") write it. |
| `method_chatzot_fixed_local` | Fixed-local Chatzot | Fixed local Chatzot | The five other strings write "fixed local Chatzot" without a hyphen. |
| `settings_advanced_zmanim` | Detailed Calculation Methods | Detailed calculation methods | Sentence case, like the sections beside it ("Zmanim to show", "Daily learning to show"). |
| `settings_language_description` | Interface language for the app’s navigation and labels. | The app's language, also used for its widgets and notification. | The old text was out of date: the language now applies to everything, widgets and notification included. It also used a curly apostrophe where every other string uses a straight one. |
| `settings_12_hour_format_description` | Show Zmanim as 6:45 AM. Off keeps 24-hour format. | Show times as 6:45 AM. Off keeps the 24-hour format. | "Zmanim" was capitalised mid-sentence and the article was missing. |
| `settings_daily_learning_section_description` | Choose which daily-study tracks appear on the Zmanim tab. | Choose which daily learning tracks appear on the Zmanim tab. | Uses the app's own term ("Daily Learning"). |
| `settings_method_alot_description` | Dawn start used for Magen Avraham and fast days. | Dawn: the start of the day for Magen Avraham and fast days. | "Dawn start" was unclear. The meaning now matches the Hebrew ("תחילת היום למג״א ולתעניות"). |
| `settings_method_sunrise_description` | Sea level is the common zmanim base; observed uses elevation. | Sea level is the usual basis for zmanim; observed takes elevation into account. | Natural phrasing, same meaning. |
| `settings_method_chatzot_description` | Solar or fixed-local midday. | Solar or fixed local midday. | Same hyphen fix as `method_chatzot_fixed_local`. |
| `settings_method_plag_description` | Earliest Shabbat or Maariv boundary. | Earliest time to accept Shabbat or pray Maariv. | "Boundary" was jargon; this says the same thing plainly. |
| `settings_method_chatzot_halaila_description` | Solar or fixed-local midnight. | Solar or fixed local midnight. | Same hyphen fix. |
| `settings_method_rabbeinu_tam_description` | Separate Rabbeinu Tam Shabbat opinion. | End of Shabbat by Rabbeinu Tam's opinion. | Says what the row is. |
| `settings_method_chametz_description` | Sof zman eating and burning chametz. | Latest times to eat and burn chametz. | It was half transliteration and half English, and ungrammatical. |
| `widget_kind_shabbat` | Shabbat & chag | Shabbat & Yom Tov | Everywhere else, the English text says "Yom Tov", not "chag". |
| `widget_shabbat_label` (widget.xml) | Minimal · Shabbat & chag · … | Minimal · Shabbat & Yom Tov · … | Same; only the English half changed. |
| `widget_glass_shabbat_label` (widget.xml) | Glassy sky · Shabbat & chag · … | Glassy sky · Shabbat & Yom Tov · … | Same. |
| `widget_shabbat_description` (widget.xml) | Candle lighting and when Shabbat or chag goes out · … | Candle lighting and when Shabbat or Yom Tov goes out · … | Same. |
| `widget_glass_shabbat_description` (widget.xml) | …when Shabbat or chag goes out, under the candle-lighting sky · … | …when Shabbat or Yom Tov goes out, under the candle-lighting sky · … | Same. |
| `mizrach_need_location` | Allow location so Prayer Compass can calculate from where you are now. | Allow location so the prayer compass can calculate from where you are now. | Running text says "the prayer compass", as the location prompt already does. The tab title stays "Prayer Compass". |
| `mizrach_location_off` | …so Prayer Compass can find where you are now. | …so the prayer compass can find where you are now. | Same. |
| `mizrach_location_accuracy_low` | Location accuracy is too low for confident alignment. Try again for a better fix. | Location accuracy is too low to show the direction reliably. Try again for a more precise location. | "Confident alignment" and "fix" are engineering jargon. |
| `mizrach_precise_location_required` | Precise location is required for confident alignment. | Precise location is needed to show the direction reliably. | Same. |
| `mizrach_retry_location` | Try location again | Try again | Awkward phrase; the message above the button already says it is about location. Matches `update_action_retry`. |
| `update_downgrade_body` | …Installing it takes you back to it. | …Installing it takes you back to the stable version. | "Back to it" was ambiguous: back to which version? |

One translator comment was updated to match: `holy_day_starts_first_day` no longer says the label
starts in lowercase. Two tests that find rows by title were updated for "Chatzot Hayom" and
"Chatzot Halaila".

## Hebrew (`values-iw/strings.xml`)

| Key | Old | New | Reason |
|---|---|---|---|
| `report_issue` | דיווח באג או בקשת פיצ׳ר | דיווח על באג או בקשת פיצ׳ר | Correct government of the noun: "דיווח על". |
| `zmanim_candle_prompt_question` | כמה דקות לפני שקיעה אתה מקבל שבת? | כמה דקות לפני השקיעה אתה מקבל שבת? | "לפני השקיעה", with the article, as a luach writes it. |
| `zmanim_candle_prompt_body` | בחר כמה דקות לפני שקיעה: | בחר כמה דקות לפני השקיעה: | Same. |
| `zmanim_day_previous` | יום קודם | היום הקודם | "יום הבא" without the article is ungrammatical, so both arrows get it. Read only by TalkBack. |
| `zmanim_day_next` | יום הבא | היום הבא | Same. |
| `zmanim_loading` | מחשב זמנים | מחשב זמנים… | Same ellipsis as "ממתין למיקום הנוכחי…". |
| `zmanim_location_jerusalem` | הזמנים מבוססים על ירושלים | הזמנים לפי ירושלים | "מבוססים על" is translationese; "לפי" is what a luach says. |
| `zmanim_location_named` | הזמנים מבוססים על %1$s | הזמנים לפי %1$s | Same. |
| `row_rosh_chodesh_description` | ראש חודש | חודש עברי חדש | The caption repeated the row's title word for word. Now it matches the English ("New Jewish month"). |
| `settings_language_description` | שפת הממשק לניווט ולתוויות האפליקציה. | שפת האפליקציה, גם בווידג׳טים ובהתראה. | Out of date, as in English: the language applies to everything. |
| `settings_12_hour_format_description` | הצג זמנים כמו 6:45 AM. כבוי משאיר פורמט 24 שעות. | הצג זמנים כמו 6:45 AM. כשכבוי, הזמנים בפורמט 24 שעות. | "כבוי משאיר" was not a sentence. |
| `settings_advanced_zmanim_description` | בחר את שיטת ההלכה המדויקת לכל זמן מוצג. | בחר את השיטה ההלכתית המדויקת לכל זמן מוצג. | "שיטת ההלכה" reads as "the method of halacha". |
| `settings_zmanim_times_section_description` | בחרו אילו זמנים יופיעו ברשימת הזמנים. | בחר אילו זמנים יופיעו ברשימת הזמנים. | The neighbouring section says "בחר" (singular), as every other instruction in the app does. |
| `settings_daily_learning_section_description` | בחרו אילו מסלולי לימוד יומי יוצגו בלשונית הזמנים. | בחר אילו מסלולי לימוד יומי יוצגו בלשונית הזמנים. | Same. |
| `settings_method_ateret_torah_offset` | עטרת תורה — דקות אחרי שקיעה | עטרת תורה — דקות אחרי השקיעה | Article, as in `zmanim_candle_prompt_question`. |
| `settings_method_sunrise_description` | מישור הוא בסיס נפוץ לזמנים; נראית משתמשת בגובה. | זריחה במישור היא הבסיס המקובל לזמנים; זריחה נראית מתחשבת בגובה. | The old text named the options by bare adjectives ("מישור", "נראית"), which don't stand on their own. |
| `settings_method_mincha_ketana_description` | תחילת זמן מנחה קטן. | תחילת זמן מנחה קטנה. | Gender: the zman is "מנחה קטנה", as in its row title. |
| `settings_method_plag_description` | גבול מוקדם לקבלת שבת או מעריב. | הזמן המוקדם ביותר לקבלת שבת או למעריב. | "גבול מוקדם" was unnatural. Same meaning, now matching the English. |
| `settings_method_candle_lighting_description` | דקות לפני שקיעה להדלקת נרות. | דקות לפני השקיעה להדלקת נרות. | Article, matching `settings_candle_lighting_dialog`. |
| `settings_method_rabbeinu_tam_description` | שיטת רבינו תם נפרדת לשבת. | צאת שבת לשיטת רבינו תם. | Says what the row is, in the luach's own phrase. |
| `settings_ateret_torah_dialog` | דקות אחרי שקיעה. חכם הררי-רפול… | דקות אחרי השקיעה. חכם הררי־רפול… | Article, and a Hebrew maqaf (־) in the name instead of an ASCII hyphen. |
| `mizrach_location_accuracy_low` | …נמוך מדי לכיוון ודאי… | …נמוך מדי לכיוון אמין… | "כיוון ודאי" is unnatural; "אמין" is what is meant. |
| `mizrach_precise_location_required` | נדרש מיקום מדויק לכיוון ודאי. | נדרש מיקום מדויק כדי להציג כיוון אמין. | Same, and it is now a full sentence. |
| `mizrach_retry_location` | נסה שוב את המיקום | נסה שוב | Awkward phrase. Same change as the English. |
| `mizrach_figure_eight_tip` | לשיפור הדיוק מומלץ לנענע את הטלפון כמה פעמים בתנועה של הספרה שמונה. | לשיפור הדיוק, הזז את הטלפון כמה פעמים בתנועת שמונה. | The three compass messages named the same movement three ways; now all say "בתנועת שמונה". |
| `mizrach_compass_accuracy_low` | …הזז את הטלפון בצורת שמונה… | …הזז את הטלפון בתנועת שמונה… | Same. |
| `mizrach_compass_unreliable` | …הזז את הטלפון בצורת שמונה… | …הזז את הטלפון בתנועת שמונה… | Same. |
| `update_available_title` | קיים עדכון | עדכון זמין | The standard phrase in Hebrew app stores and Android. |
| `update_banner_title` | קיים עדכון | עדכון זמין | Same. |
| `update_downgrade_body` | …התקנתה תחזיר אותך אליה. | …התקנתה תחזיר אותך לגרסה היציבה. | "אליה" was ambiguous, as in English. |
| `update_progress_downloaded` | ירדו %1$s מ״ב | הורדו %1$s מ״ב | "ירדו" is slang; "הורדו" is the correct passive. |
| `about_email` | צרו קשר באימייל | יצירת קשר באימייל | A row label, so a noun phrase like its neighbour "צפייה בקוד המקור ב‑GitHub". It was also the last plural-imperative outlier. |

## Considered and left alone

| Key | Text | Why left |
|---|---|---|
| Hebrew instructions throughout | בחר, הצג, נסה, פנה, הזז… | Masculine singular imperative is the app's convention and the long-standing Israeli UI norm. Rewriting every instruction gender-neutrally is a style decision for Noam, not a fix. I only removed the plural outliers. |
| Hebrew buttons | "אפשר מיקום", "הפעל מיקום" next to "שמירה", "ביטול" | Imperative and noun buttons are mixed, but both are normal in Hebrew apps. Changing them is a matter of taste. |
| `sequel_two_day_yom_tov` / `sequel_three_day_yom_tov` (he) | חג כפול / חג משולש | Clear and short in a tight line. "שני ימים טובים" is more formal but longer. |
| `fast_17_tammuz` / `fast_10_tevet` (he) | י״ז בתמוז / עשרה בטבת | Mixed numeral and spelled-out forms, but these are exactly the names people use for each fast. |
| `fast_17_tammuz` / `fast_10_tevet` (en) | 17th of Tammuz / 10th of Tevet | KosherJava's list spells them out ("Seventeenth of Tammuz"). That list is never shown on a fast day, because the app's own fast names take over. |
| `shabbat_row_candles` (en) | Candle Lighting & Shabbat Entry | "Shabbat Entry" mirrors "כניסת שבת" and reads a little foreign. A natural alternative ("…& Start of Shabbat") is longer, and the row has been this way since the start. |
| `zmanim_candle_prompt_question` (en) | …do you welcome Shabbat? | The question is about accepting Shabbat (קבלת שבת), not only lighting. Rewording it to "light candles" would change what is asked. |
| `settings_zmanim_options` | Zmanim calculation methods and zmanim to show | Says "zmanim" twice, but every alternative loses one of the two things the section holds. |
| `row_eat_chametz` / `row_burn_chametz` (en) | Eat Chametz Until / Burn Chametz Until | The usual luach phrasing in English. |
| `method_zmaniyot_minutes_value` one (en) | 1 zmaniyot | Strictly singular would be "zmanit", but only a typed-in value of 1 shows it. |
| Mixed Ha- capitalisation in names | Shemirat HaLashon, Kodesh HaKodashim, Sefirat HaOmer, Baal Hatanya | These are titles and set names with their own conventional spelling. Only the zman row titles, which sit together in one list, were unified. |
| `method_sunrise_sea_level` / `method_sunrise_observed` (he) | זריחה במישור / זריחה נראית | Method names: they identify the option and are already in use. |
| `method_chatzot_solar` (he) | חצות שמשי | Method name; left as is. |
| `about_body` (en) | "This is the app I wish every app was." | Noam's personal voice. "were" would be more formal, not more natural. |
| `mizrach_developer_location_compass` | Live alignment is disabled while using a developer location override. | Only seen in developer mode. |
| Calendar names (KosherJava / Hebcal) | Holidays, months, parshiot, masechtot, readings | Already modern (Sukkot, Shavuot, Tevet, Bereshit) and consistent. The Hebrew comes from KosherJava's own lists. |
