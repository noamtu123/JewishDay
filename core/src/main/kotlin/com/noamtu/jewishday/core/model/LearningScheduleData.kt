// SPDX-License-Identifier: GPL-3.0-or-later AND BSD-2-Clause

// The daily-learning schedules' tables: the Mishnah's chapters, the Mishneh Torah's books, the Masoretic
// sedarim of Nach, and the yearly Kitzur Shulchan Aruch and Shemirat HaLashon calendars. Taken from
// @hebcal/learning 6.13.2 (https://github.com/hebcal/hebcal-learning), Hebrew names from its he.po; only
// the data, the code that reads it is this app's own. Its licence asks that this notice travel with it:
//
// BSD 2-Clause License
//
// Copyright (c) 2026, hebcal
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// Corrected here (each was a plain typo in the source, checked against the text on Sefaria):
// Kitzur 30 Tishrei "5:9-6:16" -> "5:9-5:16" and 1 Cheshvan "6:17-6:3" -> "5:17-6:3" (siman 5 has 17 seifim);
// 3 Tishrei "133:27-4:1" -> "133:27-134:1"; 18 Tishrei "98:33-98:32" -> "98:23-98:32"; 2 Iyar "80:267-80:35" ->
// "80:27-80:35"; 5 Av "123:3-1234:3" -> "123:3-124:3"; leap-year Adar II option A day 25 "54-46" -> "54-56",
// option B day 8 "11:1-17:7" -> "17:1-17:7", day 9 "17-8:18-2" -> "17:8-18:2", day 27 "55:2-56-4" -> "55:2-56:4".

package com.noamtu.jewishday.core.model

/** Every tractate of the Mishnah in order: English name, Hebrew name, mishnayot per chapter. */
internal val MishnahTractateTable: String = """
Berakhot|ברכות|5,8,6,7,5,8,5,8,5
Peah|פאה|6,8,8,11,8,11,8,9
Demai|דמאי|4,5,6,7,11,12,8
Kilayim|כלאים|9,11,7,9,8,9,8,6,10
Sheviit|שביעית|8,10,10,10,9,6,7,11,9,9
Terumot|תרומות|10,6,9,13,9,6,7,12,7,12,10
Maasrot|מעשרות|8,8,10,6,8
Maaser Sheni|מעשר שני|7,10,13,12,15
Challah|חלה|9,8,10,11
Orlah|ערלה|9,17,9
Bikkurim|ביכורים|11,11,12,5
Shabbat|שבת|11,7,6,2,4,10,4,7,7,6,6,6,7,4,3,8,8,3,6,5,3,6,5,5
Eruvin|עירובין|10,6,9,11,9,10,11,11,4,15
Pesachim|פסחים|7,8,8,9,10,6,13,8,11,9
Shekalim|שקלים|7,5,4,9,6,6,7,8
Yoma|יומא|8,7,11,6,7,8,5,9
Sukkah|סוכה|11,9,15,10,8
Beitzah|ביצה|10,10,8,7,7
Rosh Hashanah|ראש השנה|9,9,8,9
Taanit|תענית|7,10,9,8
Megillah|מגילה|11,6,6,10
Moed Katan|מועד קטן|10,5,9
Chagigah|חגיגה|8,7,8
Yevamot|יבמות|4,10,10,13,6,6,6,6,6,9,7,6,13,9,10,7
Ketubot|כתובות|10,10,9,12,9,7,10,8,9,6,6,4,11
Nedarim|נדרים|4,5,11,8,6,10,9,7,10,8,12
Nazir|נזיר|7,10,7,7,7,11,4,2,5
Sotah|סוטה|9,6,8,5,5,4,8,7,15
Gittin|גיטין|6,7,8,9,9,7,9,10,10
Kiddushin|קידושין|10,10,13,14
Bava Kamma|בבא קמא|4,6,11,9,7,6,7,7,12,10
Bava Metzia|בבא מציעא|8,11,12,12,11,8,11,9,13,6
Bava Batra|בבא בתרא|6,14,8,9,11,8,4,8,10,8
Sanhedrin|סנהדרין|6,5,8,5,5,6,11,7,6,6,6
Makkot|מכות|10,8,16
Shevuot|שבועות|7,5,11,13,5,7,8,6
Eduyot|עדיות|14,10,12,12,7,3,9,7
Avodah Zarah|עבודה זרה|9,7,10,12,12
Avot|אבות|18,16,18,22,23,11
Horayot|הוריות|5,7,8
Zevachim|זבחים|4,5,6,6,8,7,6,12,7,8,8,6,8,10
Menachot|מנחות|4,5,7,5,9,7,6,7,9,9,9,5,11
Chullin|חולין|7,10,7,7,5,7,6,6,8,4,2,5
Bekhorot|בכורות|7,9,4,10,6,12,7,10,8
Arakhin|ערכין|4,6,5,4,6,5,5,7,8
Temurah|תמורה|6,3,5,4,6,5,6
Keritot|כריתות|7,6,10,3,8,9
Meilah|מעילה|4,9,8,6,5,6
Tamid|תמיד|4,5,9,3,6,3,4
Middot|מדות|9,6,8,7,4
Kinnim|קינים|4,5,6
Kelim|כלים|9,8,8,4,11,4,6,11,8,8,9,8,8,8,6,8,17,9,10,7,3,10,5,17,9,9,12,10,8,4
Oholot|אהלות|8,7,7,3,7,7,6,6,16,7,9,8,6,7,10,5,5,10
Negaim|נגעים|6,5,8,11,5,8,5,10,3,10,12,7,12,13
Parah|פרה|4,5,11,4,9,5,12,11,9,6,9,11
Tahorot|טהרות|9,8,8,13,9,10,9,9,9,8
Mikvaot|מקוואות|8,10,4,5,6,11,7,5,7,8
Niddah|נדה|7,7,7,7,9,14,5,4,11,8
Makhshirin|מכשירין|6,11,8,10,11,8
Zavim|זבים|6,4,3,7,12
Tevul Yom|טבול יום|5,8,6,7
Yadayim|ידים|5,4,5,8
Oktzin|עוקצים|6,10,12
"""

/** Every book of the Mishneh Torah in order: English name, Hebrew name, chapters. */
internal val MishnehTorahTable: String = """
Transmission of the Oral Law|מסירת תורה שבעל פה|3
Positive Mitzvot|מצוות עשה|3
Negative Mitzvot|מצוות לא תעשה|3
Overview of Mishneh Torah Contents|תוכן החיבור|3
Foundations of the Torah|הלכות יסודי התורה|10
Human Dispositions|הלכות דעות|7
Torah Study|הלכות תלמוד תורה|7
Foreign Worship and Customs of the Nations|הלכות עבודה זרה וחוקות הגויים|12
Repentance|הלכות תשובה|10
Reading the Shema|הלכות קריאת שמע|4
Prayer and the Priestly Blessing|הלכות תפילה וברכת כהנים|15
Tefillin, Mezuzah and the Torah Scroll|הלכות תפילין ומזוזה וספר תורה|10
Fringes|הלכות ציצית|3
Blessings|הלכות ברכות|11
Circumcision|הלכות מילה|3
The Order of Prayer|סדר התפילה|4
Sabbath|הלכות שבת|30
Eruvin|עירובין|8
Rest on the Tenth of Tishrei|הלכות שביתת עשור|3
Rest on a Holiday|הלכות שביתת יום טוב|8
Leavened and Unleavened Bread|הלכות חמץ ומצה|9
Shofar, Sukkah and Lulav|הלכות שופר וסוכה ולולב|8
Sheqel Dues|הלכות שקלים|4
Sanctification of the New Month|הלכות קידוש החודש|19
Fasts|הלכות תעניות|5
Scroll of Esther and Hanukkah|הלכות מגילה וחנוכה|4
Marriage|הלכות אישות|25
Divorce|הלכות גירושין|13
Levirate Marriage and Release|הלכות יבום וחליצה|8
Virgin Maiden|הלכות נערה בתולה|3
Woman Suspected of Infidelity|הלכות סוטה|4
Forbidden Intercourse|הלכות איסורי ביאה|22
Forbidden Foods|הלכות מאכלות אסורות|17
Ritual Slaughter|הלכות שחיטה|14
Oaths|הלכות שבועות|12
Vows|הלכות נדרים|13
Nazariteship|הלכות נזירות|10
Appraisals and Devoted Property|הלכות ערכים וחרמין|8
Diverse Species|הלכות כלאים|10
Gifts to the Poor|הלכות מתנות עניים|10
Heave Offerings|הלכות תרומות|15
Tithes|הלכות מעשרות|14
Second Tithes and Fourth Year's Fruit|הלכות מעשר שני ונטע רבעי|11
First Fruits and other Gifts to Priests Outside the Sanctuary|הלכות ביכורים ושאר מתנות כהונה שבגבולין|12
Sabbatical Year and the Jubilee|הלכות שמיטה ויובל|13
The Chosen Temple|הלכות בית הבחירה|8
Vessels of the Sanctuary and Those who Serve Therein|הלכות כלי המקדש והעובדין בו|10
Admission into the Sanctuary|הלכות ביאת מקדש|9
Things Forbidden on the Altar|הלכות איסורי המזבח|7
Sacrificial Procedure|הלכות מעשה הקרבנות|19
Daily Offerings and Additional Offerings|הלכות תמידים ומוספין|10
Sacrifices Rendered Unfit|הלכות פסולי המוקדשין|19
Service on the Day of Atonement|הלכות עבודת יום הכפורים|5
Trespass|הלכות מעילה|8
Paschal Offering|הלכות קרבן פסח|10
Festival Offering|הלכות חגיגה|3
Firstlings|הלכות בכורות|8
Offerings for Unintentional Transgressions|הלכות שגגות|15
Offerings for Those with Incomplete Atonement|הלכות מחוסרי כפרה|5
Substitution|הלכות תמורה|4
Defilement by a Corpse|הלכות טומאת מת|25
Red Heifer|הלכות פרה אדומה|15
Defilement by Leprosy|הלכות טומאת צרעת|16
Those Who Defile Bed or Seat|הלכות מטמאי משכב ומושב|13
Other Sources of Defilement|הלכות שאר אבות הטומאות|20
Defilement of Foods|הלכות טומאת אוכלים|16
Vessels|הלכות כלים|28
Immersion Pools|הלכות מקואות|11
Damages to Property|הלכות נזקי ממון|14
Theft|הלכות גניבה|9
Robbery and Lost Property|הלכות גזילה ואבידה|18
One Who Injures a Person or Property|הלכות חובל ומזיק|8
Murderer and the Preservation of Life|הלכות רוצח ושמירת נפש|13
Sales|הלכות מכירה|30
Ownerless Property and Gifts|הלכות זכייה ומתנה|12
Neighbors|הלכות שכנים|14
Agents and Partners|הלכות שלוחין ושותפין|10
Slaves|הלכות עבדים|9
Hiring|הלכות שכירות|13
Borrowing and Deposit|הלכות שאלה ופיקדון|8
Creditor and Debtor|הלכות מלווה ולווה|27
Plaintiff and Defendant|הלכות טוען ונטען|16
Inheritances|הלכות נחלות|11
The Sanhedrin and the Penalties within their Jurisdiction|הלכות סנהדרין והעונשין המסורין להם|26
Testimony|הלכות עדות|22
Rebels|הלכות ממרים|7
Mourning|הלכות אבל|14
Kings and Wars|הלכות מלכים ומלחמות|12
"""

/** The sedarim of Nach, book by book: book|seder|verses. A split seder is numbered "4.1", "4.2". */
internal val TanakhSedarimTable: String = """
Joshua|1|1:1-3:6
Joshua|2|3:7-4:23
Joshua|3|4:24-6:26
Joshua|4|6:27-8:32
Joshua|5|8:33-10:7
Joshua|6|10:8-41
Joshua|7|10:42-12:24
Joshua|8|13:1-14:14
Joshua|9|14:15-17:3
Joshua|10|17:4-18:27
Joshua|11|18:28-19:50
Joshua|12|19:51-21:40
Joshua|13|21:41-22:33
Joshua|14|22:34-24:33
Judges|1|1:1-2:6
Judges|2|2:7-3:30
Judges|3|3:31-5:30
Judges|4|5:31-6:39
Judges|5|6:40-8:2
Judges|6|8:3-9:6
Judges|7|9:7-57
Judges|8|10:1-11:31
Judges|9|11:32-13:23
Judges|10|13:24-16:2
Judges|11|16:3-18:5
Judges|12|18:6-19:19
Judges|13|19:20-20:26
Judges|14|20:27-21:25
Samuel|1|I Samuel 1:1-2:9
Samuel|2|I Samuel 2:10-3:19
Samuel|3|I Samuel 3:20-6:13
Samuel|4|I Samuel 6:14-9:1
Samuel|5|I Samuel 9:2-10:23
Samuel|6|I Samuel 10:24-12:21
Samuel|7|I Samuel 12:22-14:22
Samuel|8|I Samuel 14:23-15:16
Samuel|9|I Samuel 15:17-16:17
Samuel|10|I Samuel 16:18-17:36
Samuel|11|I Samuel 17:37-18:13
Samuel|12|I Samuel 18:14-20:3
Samuel|13|I Samuel 20:4-41
Samuel|14|I Samuel 20:42-23:3
Samuel|15|I Samuel 23:4-24:19
Samuel|16|I Samuel 24:20-25:32
Samuel|17|I Samuel 25:33-26:24
Samuel|18|I Samuel 26:25-28:23
Samuel|19|I Samuel 28:24-30:24
Samuel|20|I Samuel 30:25-II Samuel 2:6
Samuel|21|II Samuel 2:7-3:20
Samuel|22|II Samuel 3:21-5:9
Samuel|23|II Samuel 5:10-7:15
Samuel|24|II Samuel 7:16-10:11
Samuel|25|II Samuel 10:12-12:12
Samuel|26|II Samuel 12:13-13:24
Samuel|27|II Samuel 13:25-14:32
Samuel|28|II Samuel 14:33-15:36
Samuel|29|II Samuel 15:37-17:19
Samuel|30|II Samuel 17:20-18:26
Samuel|31|II Samuel 18:27-19:39
Samuel|32|II Samuel 19:40-21:6
Samuel|33|II Samuel 21:7-22:50
Samuel|34|II Samuel 22:51-24:25
Song of Songs|1|1:1-8:14
Ruth|1|1:1-4:22
Kings|1|I Kings 1:1-47
Kings|2|I Kings 1:48-2:44
Kings|3|I Kings 2:45-4:19
Kings|4|I Kings 4:20-6:12
Kings|5|I Kings 6:13-7:20
Kings|6|I Kings 7:21-8:10
Kings|7|I Kings 8:11-57
Kings|8|I Kings 8:58-10:8
Kings|9|I Kings 10:9-11:27
Kings|10|I Kings 11:28-12:23
Kings|11|I Kings 12:24-13:30
Kings|12|I Kings 13:31-15:7
Kings|13|I Kings 15:8-16:14
Kings|14|I Kings 16:15-17:23
Kings|15|I Kings 17:24-18:38
Kings|16|I Kings 18:39-20:12
Kings|17|I Kings 20:13-21:16
Kings|18|I Kings 21:17-22:42
Kings|19|I Kings 22:43-II Kings 2:14
Kings|20|II Kings 2:15-4:25
Kings|21|II Kings 4:26-6:22
Kings|22|II Kings 6:23-7:15
Kings|23|II Kings 7:16-9:12
Kings|24|II Kings 9:13-10:14
Kings|25|II Kings 10:15-12:2
Kings|26|II Kings 12:3-13:22
Kings|27|II Kings 13:23-15:6
Kings|28|II Kings 15:7-16:19
Kings|29|II Kings 16:20-18:5
Kings|30|II Kings 18:6-19:18
Kings|31|II Kings 19:19-20:7
Kings|32|II Kings 20:8-22:1
Kings|33|II Kings 22:2-23:24
Kings|34|II Kings 23:25-24:17
Kings|35|II Kings 24:18-25:30
Isaiah|1|1:1-4:2
Isaiah|2|4:3-6:2
Isaiah|3|6:3-9:5
Isaiah|4|9:6-11:1
Isaiah|5|11:2-14:1
Isaiah|6|14:2-16:4
Isaiah|7|16:5-19:24
Isaiah|8|19:25-22:22
Isaiah|9|22:23-24:22
Isaiah|10|24:23-27:12
Isaiah|11|27:13-29:22
Isaiah|12|29:23-32:17
Isaiah|13|32:18-35:9
Isaiah|14|35:10-37:19
Isaiah|15|37:20-39:8
Isaiah|16|40:1-41:26
Isaiah|17|41:27-44:5
Isaiah|18|44:6-45:16
Isaiah|19|45:17-48:1
Isaiah|20|48:2-49:25
Isaiah|21|49:26-52:6
Isaiah|22|52:7-55:12
Isaiah|23|55:13-58:13
Isaiah|24|58:14-61:8
Isaiah|25|61:9-65:8
Isaiah|26|65:9-66:24
Jeremiah|1|1:1-3:3
Jeremiah|2|3:4-4:31
Jeremiah|3|5:1-6:1
Jeremiah|4|6:2-7:22
Jeremiah|5|7:23-9:22
Jeremiah|6|9:23-12:14
Jeremiah|7|12:15-14:21
Jeremiah|8|14:22-17:6
Jeremiah|9|17:7-18:18
Jeremiah|10|18:19-20:12
Jeremiah|11|20:13-23:5
Jeremiah|12|23:6-24:6
Jeremiah|13|24:7-25:38
Jeremiah|14|26:1-27:4
Jeremiah|15|27:5-29:6
Jeremiah|16|29:7-30:8
Jeremiah|17|30:9-31:31
Jeremiah|18|31:32-32:21
Jeremiah|19|32:22-33:15
Jeremiah|20|33:16-35:9
Jeremiah|21|35:10-36:25
Jeremiah|22|36:26-38:7
Jeremiah|23|38:8-39:17
Jeremiah|24|39:18-42:11
Jeremiah|25|42:12-44:19
Jeremiah|26|44:20-46:26
Jeremiah|27|46:27-49:1
Jeremiah|28|49:2-50:4
Jeremiah|29|50:5-51:9
Jeremiah|30|51:10-58
Jeremiah|31|51:59-52:34
Ezekiel|1|1:1-3:11
Ezekiel|2|3:12-5:17
Ezekiel|3|6:1-7:27
Ezekiel|4|8:1-9:11
Ezekiel|5|10:1-11:19
Ezekiel|6|11:20-13:23
Ezekiel|7|14:1-16:13
Ezekiel|8|16:14-59
Ezekiel|9|16:60-18:8
Ezekiel|10|18:9-19:14
Ezekiel|11|20:1-40
Ezekiel|12|20:41-22:15
Ezekiel|13|22:16-23:26
Ezekiel|14|23:27-24:23
Ezekiel|15|24:24-26:19
Ezekiel|16|26:20-28:12
Ezekiel|17|28:13-29:20
Ezekiel|18|29:21-31:18
Ezekiel|19|32:1-33:15
Ezekiel|20|33:16-34:25
Ezekiel|21|34:26-36:24
Ezekiel|22|36:25-37:27
Ezekiel|23|37:28-39:21
Ezekiel|24|39:22-40:44
Ezekiel|25|40:45-42:12
Ezekiel|26|42:13-43:26
Ezekiel|27|43:27-45:14
Ezekiel|28|45:15-47:11
Ezekiel|29|47:12-48:35
Minor Prophets|1|Hosea 1:1-6:1
Minor Prophets|2|Hosea 6:2-10:11
Minor Prophets|3|Hosea 10:12-14:5
Minor Prophets|4|Hosea 14:6-Joel 2:26
Minor Prophets|5|Joel 2:27-Amos 2:9
Minor Prophets|6|Amos 2:10-5:13
Minor Prophets|7|Amos 5:14-7:14
Minor Prophets|8|Amos 7:15-Obadiah 1:20
Minor Prophets|9|Obadiah 1:21-Jonah 4:11
Minor Prophets|10|Micah 1:1-4:4
Minor Prophets|11|Micah 4:5-7:19
Minor Prophets|12|Micah 7:20-Nahum 3:19
Minor Prophets|13|Habakkuk 1:1-3:19
Minor Prophets|14|Zephaniah 1:1-3:19
Minor Prophets|15|Zephaniah 3:20-Haggai 2:22
Minor Prophets|16|Haggai 2:23-Zechariah 4:1
Minor Prophets|17|Zechariah 4:2-6:13
Minor Prophets|18|Zechariah 6:14-8:22
Minor Prophets|19|Zechariah 8:23-11:17
Minor Prophets|20|Zechariah 12:1-14:20
Minor Prophets|21|Zechariah 14:21-Malachi 3:24
Psalms|1|1:1-11:6
Psalms|2|11:7-20:9
Psalms|3|20:10-29:10
Psalms|4|29:11-35:27
Psalms|5|35:28-41:13
Psalms|6|41:14-49:18
Psalms|7|49:19-57:11
Psalms|8|57:12-67:7
Psalms|9|67:8-72:19
Psalms|10|72:20-78:37
Psalms|11|78:38-84:12
Psalms|12|84:13-90:16
Psalms|13|90:17-100:5
Psalms|14|101:1-105:44
Psalms|15|105:45-111:9
Psalms|16|111:10-119:71
Psalms|17|119:72-128:5
Psalms|18|128:6-140:13
Psalms|19|140:14-150:6
Proverbs|1|1:1-5:17
Proverbs|2|5:18-9:10
Proverbs|3|9:11-14:3
Proverbs|4|14:4-18:9
Proverbs|5|18:10-22:20
Proverbs|6|22:21-25:12
Proverbs|7|25:13-28:15
Proverbs|8|28:16-31:31
Job|1|1:1-5:26
Job|2|5:27-11:18
Job|3|11:19-17:8
Job|4|17:9-22:29
Job|5|22:30-29:13
Job|6|29:14-33:32
Job|7|33:33-38:34
Job|8|38:35-42:17
Lamentations|1|1:1-5:22
Ecclesiastes|1|1:1-3:12
Ecclesiastes|2|3:13-6:12
Ecclesiastes|3|7:1-9:6
Ecclesiastes|4|9:7-12:14
Esther|1|1:1-2:4
Esther|2|2:5-3:7
Esther|3|3:8-6:10
Esther|4|6:11-8:15
Esther|5|8:16-10:3
Daniel|1|1:1-2:34
Daniel|2|2:35-3:29
Daniel|3|3:30-5:11
Daniel|4|5:12-6:10
Daniel|5|6:11-9:3
Daniel|6|9:4-10:20
Daniel|7|10:21-12:13
Ezra and Nehemiah|1|Ezra 1:1-3:12
Ezra and Nehemiah|2|Ezra 3:13-6:17
Ezra and Nehemiah|3|Ezra 6:18-8:34
Ezra and Nehemiah|4|Ezra 8:35-Nehemiah 1:10
Ezra and Nehemiah|5|Nehemiah 1:11-3:37
Ezra and Nehemiah|6|Nehemiah 3:38-6:14
Ezra and Nehemiah|7|Nehemiah 6:15-8:9
Ezra and Nehemiah|8|Nehemiah 8:10-9:37
Ezra and Nehemiah|9|Nehemiah 10:1-12:26
Ezra and Nehemiah|10|Nehemiah 12:27-13:31
Chronicles|1|I Chronicles 1:1-4:9
Chronicles|2|I Chronicles 4:10-6:33
Chronicles|3|I Chronicles 6:34-8:39
Chronicles|4|I Chronicles 8:40-11:8
Chronicles|5|I Chronicles 11:9-12:40
Chronicles|6|I Chronicles 12:41-16:35
Chronicles|7|I Chronicles 16:36-19:12
Chronicles|8|I Chronicles 19:13-22:18
Chronicles|9|I Chronicles 22:19-26:4
Chronicles|10|I Chronicles 26:5-28:9
Chronicles|11|I Chronicles 28:10-II Chronicles 2:2
Chronicles|12|II Chronicles 2:3-5:13
Chronicles|13|II Chronicles 5:14-7:10
Chronicles|14|II Chronicles 7:11-9:23
Chronicles|15|II Chronicles 9:24-12:11
Chronicles|16|II Chronicles 12:12-15:14
Chronicles|17|II Chronicles 15:15-18:17
Chronicles|18|II Chronicles 18:18-20:29
Chronicles|19|II Chronicles 20:30-23:10
Chronicles|20|II Chronicles 23:11-26:1
Chronicles|21|II Chronicles 26:2-29:10
Chronicles|22|II Chronicles 29:11-31:9
Chronicles|23|II Chronicles 31:10-34:1
Chronicles|24|II Chronicles 34:2-35:5
Chronicles|25|II Chronicles 35:6-36:23
Joshua|4.1|6:27-7:26
Joshua|4.2|8:1-32
Jeremiah|9.1|17:7-25
Jeremiah|9.2|17:26-18:18
Song of Songs|1.1|1:1-5:1
Song of Songs|1.2|5:2-8:14
Ruth|1.1|1:1-2:11
Ruth|1.2|2:12-4:22
"""

/** Kitzur Shulchan Aruch by Hebrew date: month|day|reading ("siman:seif-siman:seif", E = to the end). */
internal val KitzurTable: String = """
Tishrei|1|133:17-133:21
Tishrei|2|133:22-133:26
Tishrei|3|133:27-134:1
Tishrei|4|134:2-134:6
Tishrei|5|134:7-134:12
Tishrei|6|134:13-135:2
Tishrei|7|135:3-135:6
Tishrei|8|135:7-135:12
Tishrei|9|135:13-135:E
Tishrei|10|136:1-136:2
Tishrei|11|136:3-136:E
Tishrei|12|137:1-137:7
Tishrei|13|137:8-138:1
Tishrei|14|138:2-138:E
Tishrei|15|98:1-98:7
Tishrei|16|98:8-98:13
Tishrei|17|98:14-98:22
Tishrei|18|98:23-98:32
Tishrei|19|98:33-99:2
Tishrei|20|99:3-100:3
Tishrei|21|100:5-100:10
Tishrei|22|100:11-100:16
Tishrei|23|100:17-100:E
Tishrei|24|1:1-1:4
Tishrei|25|1:5-2:4
Tishrei|26|2:5-3:1
Tishrei|27|3:2-3:E
Tishrei|28|4:1-5:1
Tishrei|29|5:2-5:8
Tishrei|30|5:9-5:16
Cheshvan|1|5:17-6:3
Cheshvan|2|6:4-6:9
Cheshvan|3|6:10-7:E
Cheshvan|4|8:1-8:5
Cheshvan|5|8:6-9:3
Cheshvan|6|9:4-9:9
Cheshvan|7|9:10-9:13
Cheshvan|8|9:14-9:E
Cheshvan|9|10:1-10:3
Cheshvan|10|10:4-10:12
Cheshvan|11|10:13-10:19
Cheshvan|12|10:20-10:E
Cheshvan|13|11:1-11:11
Cheshvan|14|11:12-11:20
Cheshvan|15|11:21-12:4
Cheshvan|16|12:5-12:10
Cheshvan|17|12:11-13:1
Cheshvan|18|13:2-14:3
Cheshvan|19|14:4-14:E
Cheshvan|20|15:1-15:6
Cheshvan|21|15:7-15:E
Cheshvan|22|16:1-16:E
Cheshvan|23|17:1-17:7
Cheshvan|24|17:8-18:2
Cheshvan|25|18:3-18:9
Cheshvan|26|18:10-18:14
Cheshvan|27|18:15-18:E
Cheshvan|28|19:1-19:7
Cheshvan|29|19:8-19:13
Kislev|1|19:14-20:7
Kislev|2|20:8-21:2
Kislev|3|21:3-21:8
Kislev|4|21:9-22:E
Kislev|5|23:1-23:9
Kislev|6|23:10-23:15
Kislev|7|23:16-23:22
Kislev|8|23:23-23:E
Kislev|9|24:1-24:6
Kislev|10|24:7-24:E
Kislev|11|25:1-26:2
Kislev|12|26:3-26:12
Kislev|13|26:13-26:21
Kislev|14|26:22-27:E
Kislev|15|28:1-28:10
Kislev|16|28:10-29:3
Kislev|17|29:4-29:10
Kislev|18|29:11-29:17
Kislev|19|29:18-30:3
Kislev|20|30:4-31:1
Kislev|21|31:2-32:1
Kislev|22|32:2-32:7
Kislev|23|32:8-32:18
Kislev|24|32:16-32:22
Kislev|25|139:1-139:4
Kislev|26|139:5-139:11
Kislev|27|139:12-139:19
Kislev|28|139:20-139:E
Kislev|29|32:23-32:E
Kislev|30|33:1-33:6
Tevet|1|33:7-33:E
Tevet|2|34:1-34:4
Tevet|3|34:5-34:13
Tevet|4|34:14-35:7
Tevet|5|35:8-36:10
Tevet|6|36:11-36:26
Tevet|7|36:27-37:9
Tevet|8|37:10-38:8
Tevet|9|38:9-39:1
Tevet|10|121:1-121:5
Tevet|11|39:2-40:4
Tevet|12|40:5-40:13
Tevet|13|40:14-40:E
Tevet|14|41:1-41:7
Tevet|15|41:8-42:5
Tevet|16|42:6-42:19
Tevet|17|42:20-43:3
Tevet|18|43:4-44:4
Tevet|19|44:5-44:13
Tevet|20|44:14-45:2
Tevet|21|45:3-45:8
Tevet|22|45:9-45:16
Tevet|23|45:17-46:3
Tevet|24|46:4-46:16
Tevet|25|46:17-46:29
Tevet|26|46:30-46:40
Tevet|27|46:41-47:7
Tevet|28|47:8-47:21
Tevet|29|47:22-48:5
Shevat|1|48:6-48:E
Shevat|2|49:1-49:6
Shevat|3|49:7-50:2
Shevat|4|50:3-50:10
Shevat|5|50:11-51:1
Shevat|6|51:2-51:7
Shevat|7|51:8-51:E
Shevat|8|52:1-52:7
Shevat|9|52:8-52:15
Shevat|10|52:16-53:2
Shevat|11|53:3-54:3
Shevat|12|54:4-55:1
Shevat|13|55:2-56:5
Shevat|14|56:6-57:5
Shevat|15|57:6-58:7
Shevat|16|58:8-59:1
Shevat|17|59:2-59:8
Shevat|18|59:9-59:19
Shevat|19|59:20-60:5
Shevat|20|60:6-60:13
Shevat|21|60:14-61:5
Shevat|22|61:6-62:3
Shevat|23|62:4-62:14
Shevat|24|62:15-63:1
Shevat|25|63:2-64:E
Shevat|26|65:1-65:8
Shevat|27|65:9-65:15
Shevat|28|65:16-65:22
Shevat|29|65:23-65:E
Shevat|30|66:1-66:6
Adar|1|66:7-66:10
Adar|2|66:11-67:5
Adar|3|67:6-67:E
Adar|4|68:1-68:7
Adar|5|68:8-69:1
Adar|6|69:2-69:7
Adar|7|69:8-70:E
Adar|8|71:1-71:4
Adar|9|71:5-72:4
Adar|10|72:5-72:10
Adar|11|140:1-141:3
Adar|12|141:4-141:13
Adar|13|141:14-141:21
Adar|14|141:22-142:5
Adar|15|142:6-142:E
Adar|16|72:11-72:19
Adar|17|72:20-73:4
Adar|18|73:5-73:E
Adar|19|74:1-75:3
Adar|20|75:4-75:9
Adar|21|75:10-76:4
Adar|22|76:5-76:13
Adar|23|107:1-108:3
Adar|24|108:4-109:6
Adar|25|109:7-110:5
Adar|26|110:6-110:12
Adar|27|110:13-111:6
Adar|28|111:7-111:13
Adar|29|111:14-112:4
Nisan|1|112:5-113:7
Nisan|2|113:8-114:4
Nisan|3|114:5-114:12
Nisan|4|114:13-115:3
Nisan|5|115:4-116:4
Nisan|6|116:5-116:14
Nisan|7|116:15-117:4
Nisan|8|117:5-117:11
Nisan|9|117:12-118:4
Nisan|10|118:5-118:8
Nisan|11|118:9-119:2
Nisan|12|119:3-119:5
Nisan|13|119:6-119:8
Nisan|14|119:9-119:E
Nisan|15|120:1-120:E
Nisan|16|101:1-102:1
Nisan|17|102:2-103:2
Nisan|18|103:3-103:11
Nisan|19|103:12-104:6
Nisan|20|104:7-104:E
Nisan|21|104:1-105:E
Nisan|22|76:14-76:22
Nisan|23|76:23-77:8
Nisan|24|77:9-77:15
Nisan|25|77:16-77:E
Nisan|26|78:1-78:7
Nisan|27|78:8-79:1
Nisan|28|79:2-79:E
Nisan|29|80:1-80:8
Nisan|30|80:9-80:17
Iyar|1|80:17-80:26
Iyar|2|80:27-80:35
Iyar|3|80:36-80:45
Iyar|4|80:46-80:60
Iyar|5|80:61-80:67
Iyar|6|80:68-80:76
Iyar|7|80:77-80:86
Iyar|8|80:87-81:2
Iyar|9|81:3-82:2
Iyar|10|82:3-82:8
Iyar|11|82:9-83:2
Iyar|12|83:3-84:4
Iyar|13|84:5-84:15
Iyar|14|84:16-85:3
Iyar|15|85:4-86:5
Iyar|16|86:6-87:6
Iyar|17|87:7-87:17
Iyar|18|87:18-88:1
Iyar|19|88:2-88:6
Iyar|20|88:7-88:14
Iyar|21|88:15-89:3
Iyar|22|89:4-90:4
Iyar|23|90:5-90:14
Iyar|24|90:15-91:1
Iyar|25|91:2-91:13
Iyar|26|91:14-92:2
Iyar|27|92:3-92:E
Iyar|28|93:1-94:2
Iyar|29|94:3-94:8
Sivan|1|94:9-94:19
Sivan|2|94:20-95:1
Sivan|3|95:2-95:11
Sivan|4|95:12-95:E
Sivan|5|96:1-96:5
Sivan|6|96:6-96:14
Sivan|7|96:15-97:9
Sivan|8|97:10-97:E
Sivan|9|143:1-143:9
Sivan|10|143:10-143:18
Sivan|11|143:19-144:6
Sivan|12|144:7-145:6
Sivan|13|145:7-145:20
Sivan|14|145:21-146:E
Sivan|15|147:1-148:E
Sivan|16|149:1-149:11
Sivan|17|149:12-150:5
Sivan|18|150:6-150:13
Sivan|19|150:14-151:5
Sivan|20|151:6-152:7
Sivan|21|152:8-152:E
Sivan|22|153:1-153:9
Sivan|23|153:10-154:1
Sivan|24|154:2-154:E
Sivan|25|155:1-155:6
Sivan|26|155:7-155:E
Sivan|27|156:1-157:3
Sivan|28|157:4-158:E
Sivan|29|159:1-159:6
Sivan|30|159:7-160:5
Tammuz|1|160:6-161:8
Tammuz|2|161:9-161:17
Tammuz|3|161:18-162:5
Tammuz|4|162:6-162:11
Tammuz|5|162:12-163:4
Tammuz|6|163:5-164:4
Tammuz|7|164:5-165:3
Tammuz|8|165:4-165:11
Tammuz|9|165:12-166:3
Tammuz|10|166:4-167:9
Tammuz|11|167:10-168:5
Tammuz|12|168:6-171:1
Tammuz|13|171:2-173:1
Tammuz|14|173:2-175:3
Tammuz|15|175:4-176:7
Tammuz|16|176:8-177:8
Tammuz|17|121:6-121:E
Tammuz|18|177:9-178:3
Tammuz|19|178:4-179:8
Tammuz|20|179:9-180:8
Tammuz|21|180:9-181:4
Tammuz|22|181:5-181:13
Tammuz|23|181:14-182:1
Tammuz|24|182:2-182:11
Tammuz|25|182:12-183:3
Tammuz|26|183:4-184:5
Tammuz|27|184:6-185:4
Tammuz|28|185:5-187:E
Tammuz|29|188:1-189:5
Av|1|189:6-191:E
Av|2|122:1-122:6
Av|3|122:7-122:11
Av|4|122:12-123:2
Av|5|123:3-124:3
Av|6|124:4-124:11
Av|7|124:12-124:20
Av|8|124:21-125:E
Av|9|126:1-126:E
Av|10|127:1-127:10
Av|11|127:11-127:E
Av|12|192:1-192:7
Av|13|192:8-193:5
Av|14|193:6-193:E
Av|15|194:1-194:11
Av|16|194:12-195:7
Av|17|195:8-196:1
Av|18|196:2-196:8
Av|19|196:9-196:19
Av|20|196:20-197:5
Av|21|197:6-198:3
Av|22|198:4-198:14
Av|23|198:15-199:9
Av|24|199:10-200:2
Av|25|200:3-200:9
Av|26|200:10-202:1
Av|27|202:2-202:8
Av|28|202:9-203:2
Av|29|203:3-204:5
Av|30|204:6-205:3
Elul|1|205:4-206:6
Elul|2|206:7-207:5
Elul|3|207:6-208:9
Elul|4|208:10-209:6
Elul|5|209:7-210:E
Elul|6|211:1-211:11
Elul|7|211:12-212:E
Elul|8|213:1-214:E
Elul|9|215:1-216:E
Elul|10|217:1-219:1
Elul|11|219:2-219:7
Elul|12|219:8-220:4
Elul|13|220:5-221:2
Elul|14|221:3-221:E
Elul|15|Klalim
Elul|16|128:1-128:4
Elul|17|128:5-128:11
Elul|18|128:12-128:E
Elul|19|129:1-129:7
Elul|20|129:8-129:13
Elul|21|129:14-129:19
Elul|22|129:20-129:E
Elul|23|130:1-130:E
Elul|24|131:1-131:4
Elul|25|131:5-131:9
Elul|26|131:10-131:16
Elul|27|131:17-132:E
Elul|28|133:1-133:8
Elul|29|133:9-133:15
AdarIIOptionA|1|1-4
AdarIIOptionA|2|5-9
AdarIIOptionA|3|10-14
AdarIIOptionA|4|15-18
AdarIIOptionA|5|19-23
AdarIIOptionA|6|24-28
AdarIIOptionA|7|29-31
AdarIIOptionA|8|32-34
AdarIIOptionA|9|35-E
AdarIIOptionA|10|Shmita:1-4
AdarIIOptionA|11|5-7
AdarIIOptionA|12|8-12
AdarIIOptionA|13|13-17
AdarIIOptionA|14|18-21
AdarIIOptionA|15|22-24
AdarIIOptionA|16|25-27
AdarIIOptionA|17|28-32
AdarIIOptionA|18|33-34:1
AdarIIOptionA|19|34:2-34:5
AdarIIOptionA|20|34:6-38
AdarIIOptionA|21|39-42
AdarIIOptionA|22|43-46
AdarIIOptionA|23|47-50
AdarIIOptionA|24|51-53
AdarIIOptionA|25|54-56
AdarIIOptionA|26|57-61
AdarIIOptionA|27|62-66
AdarIIOptionA|28|67-70
AdarIIOptionA|29|71-75
AdarIIOptionB|1|6:1-6:4
AdarIIOptionB|2|6:5-6:7
AdarIIOptionB|3|6:8-7:E
AdarIIOptionB|4|14:1-14:8
AdarIIOptionB|5|15:1-15:6
AdarIIOptionB|6|15:7-15:E
AdarIIOptionB|7|16:1-16:E
AdarIIOptionB|8|17:1-17:7
AdarIIOptionB|9|17:8-18:2
AdarIIOptionB|10|18:3-18:9
AdarIIOptionB|11|18:10-18:14
AdarIIOptionB|12|18:15-18:E
AdarIIOptionB|13|19:1-19:7
AdarIIOptionB|14|19:8-19:E
AdarIIOptionB|15|43:1-43:E
AdarIIOptionB|16|44:1-44:11
AdarIIOptionB|17|44:12-44:E
AdarIIOptionB|18|50:1-10
AdarIIOptionB|19|50:11-51:1
AdarIIOptionB|20|51:2-51:7
AdarIIOptionB|21|51:8-51:E
AdarIIOptionB|22|52:1-52:7
AdarIIOptionB|23|52:8-52:15
AdarIIOptionB|24|52:16-53:2
AdarIIOptionB|25|53:3-54:3
AdarIIOptionB|26|54:4-55:1
AdarIIOptionB|27|55:2-56:4
AdarIIOptionB|28|56:5-57:1
AdarIIOptionB|29|57:2-57:E
"""

/**
 * Shemirat HaLashon by Hebrew date (KosherJava month numbers): plain-year day/month, leap-year day/month,
 * book, section (H introduction, Z remembering, V discerning, T Torah, E epilogue, C book II's chapters),
 * first and last piece. Two rows on one date are read together.
 */
internal val ShemiratHaLashonTable: String = """
1|7|1|7|1|H|1|2
2|7|2|7|1|H|3|4
3|7|3|7|1|H|5|6
4|7|4|7|1|H|7|13
5|7|5|7|1|H|14|15
6|7|6|7|1|H|16|
7|7|7|7|1|H|17|
8|7|8|7|1|H|18|20
9|7|9|7|1|H|21|22
10|7|10|7|1|H|23|24
11|7|11|7|1|Z|1.1|1.4
12|7|12|7|1|Z|1.5|1.6
13|7|13|7|1|Z|1.7|
14|7|14|7|1|Z|2.1|2.5
15|7|15|7|1|Z|2.6|2.8
15|7|16|7|1|Z|2.9|
16|7|17|7|1|Z|2.10|2.11
16|7|18|7|1|Z|2.12|
17|7|19|7|1|Z|2.13|2.14
18|7|20|7|1|Z|3.1|3.5
18|7|21|7|1|Z|3.6|
19|7|22|7|1|Z|3.7|3.8
20|7|23|7|1|Z|4.1|4.2
20|7|24|7|1|Z|4.3|4.4
21|7|25|7|1|Z|4.5|4.6
22|7|26|7|1|Z|4.7|4.9
23|7|27|7|1|Z|4.10|4.16
24|7|28|7|1|Z|4.17|4.19
25|7|29|7|1|Z|5.1|5.4
26|7|30|7|1|Z|5.5|5.6
27|7|1|8|1|Z|5.7|
27|7|2|8|1|Z|5.8|5.10
28|7|3|8|1|Z|6.1|6.4
29|7|4|8|1|Z|6.5|6.6
29|7|5|8|1|Z|6.7|6.9
30|7|6|8|1|Z|6.10|
1|8|7|8|1|Z|7.1|7.7
2|8|8|8|1|Z|7.8|7.11
3|8|9|8|1|Z|7.12|7.15
4|8|10|8|1|Z|7.16|7.17
5|8|11|8|1|Z|7.18|7.19
6|8|12|8|1|Z|8.1|8.4
7|8|13|8|1|Z|8.5|
7|8|14|8|1|Z|8.6|
8|8|15|8|1|Z|8.7|8.9
9|8|16|8|1|Z|8.10|8.13
10|8|17|8|1|Z|9.1|9.4
11|8|18|8|1|Z|9.5|
12|8|19|8|1|Z|9.6|9.7
13|8|20|8|1|Z|10.1|10.2
14|8|21|8|1|Z|10.3|
15|8|22|8|1|Z|10.4|10.6
16|8|23|8|1|Z|11.1|11.2
16|8|24|8|1|Z|11.3|11.6
17|8|25|8|1|Z|11.7|11.9
18|8|26|8|1|Z|11.10|
19|8|27|8|1|Z|11.11|
20|8|28|8|1|Z|11.12|11.15
21|8|29|8|1|Z|12.1|12.3
22|8|30|8|1|Z|12.4|
23|8|1|9|1|Z|12.5|12.6
24|8|2|9|1|Z|13.1|13.3
25|8|3|9|1|Z|13.4|
26|8|4|9|1|Z|13.5|
27|8|5|9|1|Z|13.6|13.7
28|8|6|9|1|Z|14.1|14.2
29|8|7|9|1|Z|14.3|14.4
30|8|8|9|1|Z|14.5|
1|9|9|9|1|Z|15.1|15.3
2|9|10|9|1|Z|15.4|15.7
3|9|11|9|1|Z|15.8|15.10
3|9|12|9|1|Z|15.11|
4|9|13|9|1|Z|15.12|15.15
5|9|14|9|1|Z|16.1|16.2
6|9|15|9|1|Z|16.3|16.4
7|9|16|9|1|Z|16.5|
8|9|17|9|1|Z|16.6|
9|9|18|9|1|Z|16.7|
10|9|19|9|1|Z|17.1|17.2
10|9|20|9|1|Z|17.3|
11|9|21|9|1|Z|17.4|17.6
12|9|22|9|1|Z|17.7|
13|9|23|9|1|V|1.1|1.2
14|9|24|9|1|V|1.3|
15|9|25|9|1|V|1.4|
16|9|26|9|1|V|2.1|2.2
17|9|27|9|1|V|2.3|
18|9|28|9|1|V|2.4|
19|9|29|9|1|V|2.5|
20|9|30|9|1|V|2.6|2.10
21|9|1|10|1|V|3.1|3.2
22|9|2|10|1|V|3.3|
23|9|3|10|1|V|3.4|3.5
24|9|4|10|1|V|3.6|
25|9|5|10|1|V|3.7|3.8
26|9|6|10|1|V|4.1|4.3
27|9|7|10|1|V|4.4|
28|9|8|10|1|V|4.5|4.6
29|9|9|10|1|V|5.1|5.2
30|9|10|10|1|V|5.3|5.4
1|10|11|10|1|V|5.5|5.6
2|10|12|10|1|V|6.1|6.5
3|10|13|10|1|V|6.6|
4|10|14|10|1|V|6.7|6.8
5|10|15|10|1|V|6.9|6.13
6|10|16|10|1|V|7.1|7.2
7|10|17|10|1|V|7.3|7.5
8|10|18|10|1|V|7.6|7.9
9|10|19|10|1|V|7.10|7.11
10|10|20|10|1|V|7.12|
11|10|21|10|1|V|8.1|8.3
12|10|22|10|1|V|8.4|8.5
13|10|23|10|1|V|8.6|8.7
14|10|24|10|1|V|8.8|8.9
15|10|25|10|1|V|8.10|8.12
16|10|26|10|1|V|8.13|
17|10|27|10|1|V|9.1|9.2
18|10|28|10|1|V|9.3|
19|10|29|10|1|V|9.4|
20|10|1|11|1|V|9.5|9.6
21|10|2|11|1|V|10.1|10.3
22|10|3|11|1|V|10.4|
23|10|4|11|1|V|10.5|
24|10|5|11|1|V|10.6|
25|10|6|11|1|V|11.1|11.2
26|10|7|11|1|V|11.3|11.4
27|10|8|11|1|V|11.5|11.7
28|10|9|11|1|V|12.1|12.2
29|10|10|11|1|V|12.3|
1|11|11|11|1|V|12.4|
2|11|12|11|1|V|12.5|12.6
3|11|13|11|1|V|13.1|13.2
4|11|14|11|1|V|13.3|13.4
5|11|15|11|1|V|13.5|
5|11|16|11|1|V|13.6|
6|11|17|11|1|V|13.7|13.8
7|11|18|11|1|V|13.9|
8|11|19|11|1|V|13.10|
9|11|20|11|1|V|13.11|13.13
10|11|21|11|1|V|14.1|14.3
11|11|22|11|1|V|14.4|
12|11|23|11|1|V|14.5|14.6
13|11|24|11|1|V|14.7|14.8
14|11|25|11|1|V|15.1|15.3
15|11|26|11|1|V|15.4|15.5
16|11|27|11|1|V|15.6|
17|11|28|11|1|V|16.1|16.3
18|11|29|11|1|V|16.4|16.8
19|11|30|11|1|V|16.9|16.11
20|11|1|12|1|V|16.12|
21|11|2|12|1|V|16.13|
22|11|3|12|1|V|16.14|16.15
23|11|4|12|1|V|17.1|17.4
23|11|5|12|1|V|17.5|17.6
24|11|6|12|1|V|17.7|17.9
25|11|7|12|1|V|17.10|
26|11|8|12|1|V|17.11|17.12
27|11|9|12|1|T|1.1|1.3
28|11|10|12|1|T|1.4|
29|11|11|12|1|T|1.5|1.7
30|11|12|12|1|T|1.8|1.9
1|12|13|12|1|T|1.10|
2|12|14|12|1|T|1.11|1.12
3|12|15|12|1|T|1.13|
4|12|16|12|1|T|1.14|1.16
5|12|17|12|1|T|2.1|2.3
6|12|18|12|1|T|2.4|2.9
7|12|19|12|1|T|2.10|
8|12|20|12|1|T|2.11|
9|12|21|12|1|T|2.12|2.16
10|12|22|12|1|T|2.17|2.19
10|12|23|12|1|T|2.20|2.23
11|12|24|12|1|T|2.24|2.25
11|12|25|12|1|T|2.26|2.27
12|12|26|12|1|T|3.1|3.3
13|12|27|12|1|T|3.4|3.6
13|12|28|12|1|T|3.7|
14|12|29|12|1|T|3.8|
15|12|30|12|1|T|3.9|3.12
16|12|1|13|1|T|3.13|3.14
17|12|2|13|1|T|3.15|
18|12|3|13|1|T|3.16|3.19
19|12|4|13|1|T|4.1|4.3
20|12|5|13|1|T|4.4|4.5
21|12|6|13|1|T|4.6|4.7
22|12|7|13|1|T|4.8|4.11
23|12|8|13|1|T|4.12|4.16
24|12|9|13|1|T|4.17|4.19
25|12|10|13|1|T|4.20|4.25
26|12|11|13|1|T|4.26|
27|12|12|13|1|T|4.27|4.29
28|12|13|13|1|T|5.1|5.2
29|12|14|13|1|T|5.3|5.4
1|1|15|13|1|T|5.5|
2|1|16|13|1|T|5.6|5.8
3|1|17|13|1|T|5.9|5.11
4|1|18|13|1|T|5.12|5.13
5|1|19|13|1|T|5.14|5.15
6|1|20|13|1|T|5.16|5.17
7|1|21|13|1|T|6.1|6.3
8|1|22|13|1|T|6.4|6.5
9|1|23|13|1|T|6.6|6.9
10|1|24|13|1|T|6.10|6.14
11|1|25|13|1|T|6.15|6.16
12|1|26|13|1|T|7.1|7.2
13|1|27|13|1|T|7.3|7.4
14|1|28|13|1|T|7.5|7.6
15|1|29|13|1|T|7.7|7.8
16|1|1|1|1|T|7.9|
17|1|2|1|1|T|7.10|7.11
18|1|3|1|1|T|7.12|7.14
19|1|4|1|1|T|7.15|7.17
20|1|5|1|1|T|7.18|
21|1|6|1|1|T|7.19|
22|1|7|1|1|T|8.1|8.3
23|1|8|1|1|T|8.4|
24|1|9|1|1|T|8.5|8.6
25|1|10|1|1|T|8.7|
26|1|11|1|1|T|8.8|
27|1|12|1|1|T|8.9|8.10
28|1|13|1|1|T|9.1|9.5
29|1|14|1|1|T|9.6|9.7
30|1|15|1|1|T|9.8|9.11
1|2|16|1|1|T|9.12|
2|2|17|1|1|T|9.13|9.14
3|2|18|1|1|T|9.15|9.17
4|2|19|1|1|T|10.1|10.4
5|2|20|1|1|T|10.5|10.7
6|2|21|1|1|T|10.8|10.9
7|2|22|1|1|T|10.10|10.11
8|2|23|1|1|E|1.1|1.4
9|2|24|1|1|E|1.5|
10|2|25|1|1|E|2.1|2.4
11|2|26|1|1|E|2.5|
12|2|27|1|1|E|3.1|3.2
13|2|28|1|1|E|3.3|3.4
14|2|29|1|1|E|3.5|
15|2|30|1|1|E|3.6|
16|2|1|2|1|E|4.1|4.3
17|2|2|2|1|E|4.4|4.5
18|2|3|2|1|E|4.6|4.8
19|2|4|2|1|E|5.1|5.3
20|2|5|2|1|E|6.1|6.2
21|2|6|2|1|E|6.3|6.4
22|2|7|2|1|E|6.5|6.6
23|2|8|2|1|E|6.7|6.9
24|2|9|2|1|E|6.10|6.11
25|2|10|2|1|E|7.1|7.5
26|2|11|2|1|E|7.6|
27|2|12|2|1|E|7.7|7.8
28|2|13|2|1|E|7.9|
29|2|14|2|1|E|7.10|
1|3|15|2|1|E|7.11|
2|3|16|2|1|E|7.12|7.13
3|3|17|2|2|C|1.1|1.2
4|3|18|2|2|C|1.3|1.5
5|3|19|2|2|C|1.6|1.7
6|3|20|2|2|C|1.8|
7|3|21|2|2|C|2.1|2.4
8|3|22|2|2|C|2.5|2.6
9|3|23|2|2|C|2.7|
10|3|24|2|2|C|2.8|
10|3|25|2|2|C|2.9|
11|3|26|2|2|C|2.10|
12|3|27|2|2|C|2.11|
13|3|28|2|2|C|3.1|3.2
14|3|29|2|2|C|3.3|3.4
15|3|1|3|2|C|3.5|3.6
16|3|2|3|2|C|3.7|
17|3|3|3|2|C|4.1|4.5
18|3|4|3|2|C|4.6|4.7
19|3|5|3|2|C|5.1|5.3
20|3|6|3|2|C|5.4|5.5
21|3|7|3|2|C|5.6|5.7
22|3|8|3|2|C|6.1|6.3
23|3|9|3|2|C|6.4|6.5
24|3|10|3|2|C|7.1|7.3
25|3|11|3|2|C|7.4|
25|3|12|3|2|C|7.5|7.6
26|3|13|3|2|C|7.7|7.9
27|3|14|3|2|C|8.1|8.3
28|3|15|3|2|C|8.4|8.5
29|3|16|3|2|C|8.6|8.7
30|3|17|3|2|C|8.8|
1|4|18|3|2|C|9.1|9.5
2|4|19|3|2|C|9.6|9.7
3|4|20|3|2|C|9.8|9.10
4|4|21|3|2|C|10.1|10.4
5|4|22|3|2|C|11.1|11.3
5|4|23|3|2|C|11.4|11.6
6|4|24|3|2|C|11.7|11.8
7|4|25|3|2|C|11.9|11.10
8|4|26|3|2|C|11.11|11.12
9|4|27|3|2|C|11.13|
10|4|28|3|2|C|11.14|11.17
11|4|29|3|2|C|11.18|11.19
12|4|30|3|2|C|11.20|11.21
13|4|1|4|2|C|11.22|11.25
14|4|2|4|2|C|12.1|12.3
15|4|3|4|2|C|12.4|
16|4|4|4|2|C|12.5|
17|4|5|4|2|C|12.6|
18|4|6|4|2|C|12.7|
18|4|7|4|2|C|12.8|
19|4|8|4|2|C|12.9|12.10
19|4|9|4|2|C|12.11|
20|4|10|4|2|C|13.1|13.4
21|4|11|4|2|C|13.5|13.7
22|4|12|4|2|C|13.8|13.13
23|4|13|4|2|C|14.1|14.4
24|4|14|4|2|C|14.5|14.7
25|4|15|4|2|C|15.1|15.3
26|4|16|4|2|C|15.4|15.6
27|4|17|4|2|C|15.7|15.8
28|4|18|4|2|C|16.1|16.4
29|4|19|4|2|C|16.5|
29|4|20|4|2|C|16.6|16.10
1|5|21|4|2|C|16.11|16.12
2|5|22|4|2|C|16.13|
3|5|23|4|2|C|16.14|
3|5|24|4|2|C|16.15|
4|5|25|4|2|C|17.1|17.2
5|5|26|4|2|C|17.3|
6|5|27|4|2|C|17.4|
6|5|28|4|2|C|17.5|
7|5|29|4|2|C|18.1|18.7
8|5|1|5|2|C|19.1|19.2
9|5|2|5|2|C|19.3|19.4
10|5|3|5|2|C|19.5|19.6
11|5|4|5|2|C|19.7|19.8
12|5|5|5|2|C|19.9|19.10
13|5|6|5|2|C|19.11|19.12
14|5|7|5|2|C|19.Footnote_in_11|
15|5|8|5|2|C|19.12|19.14
15|5|9|5|2|C|19.15|
16|5|10|5|2|C|20.1|20.4
17|5|11|5|2|C|20.5|20.6
18|5|12|5|2|C|21.1|21.4
19|5|13|5|2|C|21.5|21.10
20|5|14|5|2|C|21.11|21.13
21|5|15|5|2|C|21.14|
22|5|16|5|2|C|21.15|
23|5|17|5|2|C|22.1|22.3
24|5|18|5|2|C|22.4|
25|5|19|5|2|C|22.5|22.6
26|5|20|5|2|C|23.1|23.2
27|5|21|5|2|C|23.3|
27|5|22|5|2|C|23.4|23.5
28|5|23|5|2|C|24.1|24.3
29|5|24|5|2|C|25.1|25.3
30|5|25|5|2|C|26.1|26.4
1|6|26|5|2|C|26.5|
2|6|27|5|2|C|26.6|26.11
3|6|28|5|2|C|27.1|27.2
4|6|29|5|2|C|27.3|
4|6|30|5|2|C|27.4|
5|6|1|6|2|C|27.5|
6|6|2|6|2|C|27.6|27.7
7|6|3|6|2|C|28.1|28.2
8|6|4|6|2|C|28.3|
9|6|5|6|2|C|29.1|29.8
10|6|6|6|2|C|29.9|29.13
11|6|7|6|2|C|29.14|29.16
12|6|8|6|2|C|29.17|
13|6|9|6|2|C|29.18|29.20
14|6|10|6|2|C|30.1|30.5
15|6|11|6|2|C|30.6|30.9
16|6|12|6|2|C|30.10|30.12
17|6|13|6|2|C|30.13|30.14
18|6|14|6|2|C|30.15|30.16
19|6|15|6|2|C|30.17|
19|6|16|6|2|C|30.18|30.20
20|6|17|6|2|E|1.1|1.2
21|6|18|6|2|E|1.3|
22|6|19|6|2|E|1.4|1.6
23|6|20|6|2|E|1.7|
23|6|21|6|2|E|1.8|
24|6|22|6|2|E|1.9|1.12
25|6|23|6|2|E|1.13|1.16
26|6|24|6|2|E|1.17|
26|6|25|6|2|E|1.18|
26|6|26|6|2|E|1.19|
27|6|27|6|2|E|2.1|2.2
28|6|28|6|2|E|3.1|3.4
29|6|29|6|2|E|4.1|4.2
"""
