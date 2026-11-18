package app.geeflow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import app.geeflow.domain.ClockThemeSchedule
import app.geeflow.domain.ClockThemeStage
import app.geeflow.domain.MinutesPerDay
import app.geeflow.platform.ForegroundEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

enum class CoffeeDayPhase(val dark: Boolean) {
    Morning(false), Day(false), Evening(true), Night(true),
}

/** The same ambient colors are drawn by Compose and the iOS glass host. */
data class CoffeeAmbientColors(val base: Color, val glow: Color, val cool: Color)

internal fun coffeeAmbientColors(phase: CoffeeDayPhase) = CoffeeAmbientColors(
    base = when (phase) {
        CoffeeDayPhase.Evening -> Color(0xFF060B18)
        CoffeeDayPhase.Night -> Color.Black
        else -> Color(0xFFFAFCFF)
    },
    glow = when (phase) {
        CoffeeDayPhase.Morning -> Color(0x3BFF865D)
        CoffeeDayPhase.Day -> Color(0x42FF5A36)
        CoffeeDayPhase.Evening -> Color(0xE61A3771)
        CoffeeDayPhase.Night -> Color(0xE60B2B68)
    },
    cool = if (phase.dark) Color(0xB80B142D) else Color(0xD9D8E9FF),
)

data class CoffeeClockAppearance(val phase: CoffeeDayPhase, val ambient: CoffeeAmbientColors)

private val blackout = CoffeeAmbientColors(Color.Black, Color.Transparent, Color.Transparent)
private data class AmbientAnchor(val minute: Int, val colors: CoffeeAmbientColors)

private val stageColors = listOf(
    blackout,
    CoffeeAmbientColors(Color(0xFFD9E7D0), Color(0x9978AA77), Color(0xBFADCDB6)),
    CoffeeAmbientColors(Color(0xFFF4F2E9), Color(0x70F2D3A0), Color(0xA7B8D3BE)),
    CoffeeAmbientColors(Color(0xFFD6AC91), Color(0xA8F59256), Color(0x9B9B8996)),
    coffeeAmbientColors(CoffeeDayPhase.Evening),
    blackout,
)

/** Interpolate the palette through the selected times, including schedules crossing midnight. */
fun coffeeClockAppearanceAt(
    localMinute: Float,
    schedule: ClockThemeSchedule = ClockThemeSchedule.Default,
): CoffeeClockAppearance {
    require(localMinute >= 0f && localMinute < MinutesPerDay)
    val elapsed = (localMinute - schedule[ClockThemeStage.Dawn] + MinutesPerDay) % MinutesPerDay
    val anchors = ClockThemeStage.entries.map { stage ->
        AmbientAnchor(schedule.elapsedFromDawn(stage), stageColors[stage.ordinal])
    }
    val index = anchors.indexOfFirst { elapsed < it.minute }
    if (index < 0) return CoffeeClockAppearance(CoffeeDayPhase.Night, blackout)
    val start = anchors[index - 1]
    val end = anchors[index]
    val ambient = if (start.colors == end.colors || elapsed == start.minute.toFloat()) {
        start.colors
    } else {
        val fraction = (elapsed - start.minute) / (end.minute - start.minute)
        val eased = fraction * fraction * (3f - 2f * fraction)
        CoffeeAmbientColors(
            base = lerp(start.colors.base, end.colors.base, eased),
            glow = fadeGlow(start.colors.glow, end.colors.glow, eased),
            cool = fadeGlow(start.colors.cool, end.colors.cool, eased),
        )
    }
    val phase = when {
        ambient == blackout -> CoffeeDayPhase.Night
        // Switch the controls' contrast once the backdrop itself is dim.
        ambient.base.luminance() < 0.22f -> CoffeeDayPhase.Evening
        elapsed < schedule.elapsedFromDawn(ClockThemeStage.Midday) -> CoffeeDayPhase.Morning
        else -> CoffeeDayPhase.Day
    }
    return CoffeeClockAppearance(phase, ambient)
}

private fun fadeGlow(from: Color, to: Color, fraction: Float): Color {
    // Keep the visible hue when the other endpoint has no glow.
    val start = if (from.alpha == 0f) to.copy(alpha = 0f) else from
    val end = if (to.alpha == 0f) from.copy(alpha = 0f) else to
    return lerp(start, end, fraction)
}

@Composable
fun rememberCoffeeClockAppearance(schedule: ClockThemeSchedule = ClockThemeSchedule.Default): CoffeeClockAppearance {
    var now by remember { mutableStateOf(Clock.System.now()) }
    ForegroundEffect(Unit) { now = Clock.System.now() }
    LaunchedEffect(Unit) {
        while (isActive) {
            now = Clock.System.now()
            delay(30_000L - now.toEpochMilliseconds().mod(30_000L))
        }
    }
    return remember(now, schedule) {
        val time = now.toLocalDateTime(TimeZone.currentSystemDefault())
        coffeeClockAppearanceAt(time.hour * 60 + time.minute + time.second / 60f, schedule)
    }
}

/** Neutral surfaces keep charts legible throughout the day. */
fun coffeeColorScheme(phase: CoffeeDayPhase): ColorScheme {
    val night = phase == CoffeeDayPhase.Night
    return if (!phase.dark) {
        lightColorScheme(
            primary = Color(0xFF18232F), onPrimary = Color.White,
            primaryContainer = Color(0xFFE3EAF4), onPrimaryContainer = Color(0xFF192B44),
            secondary = Color(0xFF3B5E8F), onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0EAF9), onSecondaryContainer = Color(0xFF1D3E69),
            tertiary = Color(0xFF3C677A), onTertiary = Color.White,
            tertiaryContainer = Color(0xFFDCECF5), onTertiaryContainer = Color(0xFF203F50),
            background = Color(0xFFFAFCFF),
            onBackground = Color(0xFF15202D), surface = Color(0xFFFCFDFF),
            onSurface = Color(0xFF15202D), onSurfaceVariant = Color(0xFF38495F),
            surfaceVariant = Color(0xFFE5EBF3), surfaceDim = Color(0xFFDDE5EF),
            surfaceBright = Color.White, surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF6F9FD), surfaceContainer = Color(0xFFEDF2F8),
            surfaceContainerHigh = Color(0xFFE1E9F4), surfaceContainerHighest = Color(0xFFD8E2EF),
            outline = Color(0xFF7F90A4), outlineVariant = Color(0xFFD6E0ED),
            error = Color(0xFFBA422D), onError = Color.White,
            errorContainer = Color(0xFFFFE1D9), onErrorContainer = Color(0xFF7B291C),
        )
    } else {
        darkColorScheme(
            primary = Color(0xFFD5E4FF), onPrimary = Color(0xFF12223C),
            primaryContainer = Color(0xFF1C3356), onPrimaryContainer = Color(0xFFDCE9FF),
            secondary = Color(0xFF9DBBE8), onSecondary = Color(0xFF122640),
            secondaryContainer = Color(0xFF182D4D), onSecondaryContainer = Color(0xFFD5E5FF),
            tertiary = Color(0xFF91BDD5), onTertiary = Color(0xFF102936),
            tertiaryContainer = Color(0xFF183446), onTertiaryContainer = Color(0xFFD7EEFF),
            background = if (night) Color.Black else Color(0xFF060B18),
            onBackground = Color(0xFFE2EAF8), surface = Color(0xFF0C1422),
            onSurface = Color(0xFFE2EAF8), onSurfaceVariant = Color(0xFFA0B0C8),
            surfaceVariant = Color(0xFF202E44), surfaceDim = Color.Black,
            surfaceBright = Color(0xFF24344C), surfaceContainerLowest = Color(0xFF03070E),
            surfaceContainerLow = Color(0xFF0C1422),
            surfaceContainer = Color(0xFF111D30), surfaceContainerHigh = Color(0xFF1B2C47),
            surfaceContainerHighest = Color(0xFF293D5B), outline = Color(0xFF7B91B0),
            outlineVariant = Color(0xFF2C3F5C), error = Color(0xFFFFA28A), onError = Color(0xFF522116),
            errorContainer = Color(0xFF592C24), onErrorContainer = Color(0xFFFFDDD3),
        )
    }
}
