package com.tfilaapp

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * Temporary place for hardcoded UI text.
 * Later you can replace this with values loaded from a decrypted file or resources.
 */
object TextConstants {
    const val USER_NAME = "תפילה"

    const val KARAOKE_TEXT: String =
        "ורם איפסום או בקיצור ליפסום, הוא מלל מקובל וחסר משמעות המשמש \"ממלא מקום\" בעת עריכה, בתחום הדפוס, ההדפסה והפרסום. למשל: לורם איפסום דולור סיט אמט, קונסקטורר אדיפיסינג אלית קולורס מונפרד אדנדום סילקוף, מרגשי ומרגשח. עמחליף לפרומי בלוף קינץ תתיח לרעח. לת צשחמי צש בליא, מנסוטו צמלח לביקו ננבי, צמוקו בלוקריה שיצמה ברורק. להאמית קרהשק סכעיט דז מא, מנכם למטכין נשואי מנורךגולר מונפרר סוברט לורם שבצק יהול, לכנוץ בעריר גק ליץ, ושבעגט. ושבעגט לבם סולגק. בראיט ולחת צורק מונחף, בגורמי מגמש. תרבנך וסתעד לכנו סתשם השמה - לתכי מורגם בורק? לתיג ישבעס."

    /**
     * Hardcoded schedule date/time (temporary).
     * ISO-8601 format so it's easy to parse into LocalDateTime.
     * Example: 2026-03-10T18:30
     */
    const val TARGET_DATE_TIME_ISO: String = "2026-03-10T20:00"

    /**
     * Converts ISO-8601 date-time text into a [LocalDateTime].
     * Returns null if the text is blank or invalid.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun toLocalDateTime(text: String): LocalDateTime? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null

        return try {
            LocalDateTime.parse(trimmed)
        } catch (_: DateTimeParseException) {
            null
        }
    }

}

