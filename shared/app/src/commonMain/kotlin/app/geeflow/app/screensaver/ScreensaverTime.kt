package app.geeflow.app.screensaver

internal data class ScreensaverTime(val digits: String, val isAm: Boolean)

/** Keeps the AM/PM label correct for localized 12-hour strings and 24-hour system clocks. */
internal fun parseScreensaverTime(formattedTime: String): ScreensaverTime {
    val normalized = formattedTime.map { char -> char.digitToIntOrNull()?.let { '0' + it } ?: char }.joinToString("")
    val match = ClockDigits.find(normalized) ?: return ScreensaverTime("--:--", true)
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return ScreensaverTime("--:--", true)
    val lower = normalized.lowercase()
    val explicitAm = lower.contains("am") || lower.contains("a.m.") || lower.contains("오전")
    val explicitPm = lower.contains("pm") || lower.contains("p.m.") || lower.contains("오후")
    val isAm = when { explicitAm -> true; explicitPm -> false; else -> hour < 12 }
    val hour12 = ((hour + 11) % 12 + 1).toString().padStart(2, '0')
    return ScreensaverTime(hour12 + ":" + minute.toString().padStart(2, '0'), isAm)
}

private val ClockDigits = Regex("""(\d{1,2})[:.](\d{2})""")
