package app.geeflow.domain

const val MinutesPerDay = 24 * 60

enum class ClockThemeStage(val defaultMinute: Int) {
    Dawn(4 * 60 + 30),
    Morning(8 * 60),
    Midday(12 * 60),
    Sunset(15 * 60 + 30),
    Evening(18 * 60 + 30),
    Blackout(22 * 60),
}

/** Daily local times, in chronological order starting at dawn. */
@ConsistentCopyVisibility
data class ClockThemeSchedule private constructor(private val minutes: List<Int>) {
    operator fun get(stage: ClockThemeStage): Int = minutes[stage.ordinal]

    fun elapsedFromDawn(stage: ClockThemeStage): Int = (get(stage) - get(ClockThemeStage.Dawn)).mod(MinutesPerDay)

    fun withTime(stage: ClockThemeStage, minute: Int): ClockThemeSchedule? =
        fromTimes(minutes.mapIndexed { index, previous -> if (index == stage.ordinal) minute else previous })

    fun encode(): String = minutes.joinToString(",")

    companion object {
        val Default = ClockThemeSchedule(ClockThemeStage.entries.map { it.defaultMinute })

        fun decode(value: String?): ClockThemeSchedule {
            val times = value?.split(',')?.map { it.toIntOrNull() ?: return Default } ?: return Default
            return fromTimes(times) ?: Default
        }

        private fun fromTimes(times: List<Int>): ClockThemeSchedule? {
            if (times.size != ClockThemeStage.entries.size || times.any { it !in 0 until MinutesPerDay }) return null
            val elapsed = times.map { (it - times.first()).mod(MinutesPerDay) }
            if (elapsed.zipWithNext().any { (earlier, later) -> earlier >= later }) return null
            return ClockThemeSchedule(times)
        }
    }
}
