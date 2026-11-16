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
import app.geeflow.platform.ForegroundEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

enum class CoffeeDayPhase(val dark: Boolean) {
    Morning(false), Day(false), Evening(true), Night(true);

    companion object {
        fun atHour(hour: Int): CoffeeDayPhase {
            require(hour in 0..23)
            return when (hour) {
                in 6..11 -> Morning
                in 12..17 -> Day
                in 18..22 -> Evening
                else -> Night
            }
        }
    }
}

private fun localPhase() = CoffeeDayPhase.atHour(
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour,
)

@Composable
fun rememberCoffeeDayPhase(): CoffeeDayPhase {
    var phase by remember { mutableStateOf(localPhase()) }
    ForegroundEffect(Unit) { phase = localPhase() }
    LaunchedEffect(Unit) {
        while (isActive) {
            phase = localPhase()
            delay(30_000)
        }
    }
    return phase
}

/** Neutral surfaces keep charts legible throughout the day. */
fun coffeeColorScheme(phase: CoffeeDayPhase): ColorScheme {
    val morning = phase == CoffeeDayPhase.Morning
    val night = phase == CoffeeDayPhase.Night
    return if (!phase.dark) {
        lightColorScheme(
            primary = Color(0xFF343A36), onPrimary = Color(0xFFFCFBF7),
            primaryContainer = Color(0xFFDEE5DB), onPrimaryContainer = Color(0xFF29372D),
            secondary = Color(0xFF81634B), onSecondary = Color.White,
            secondaryContainer = Color(0xFFF0E2D3), onSecondaryContainer = Color(0xFF574331),
            tertiary = Color(0xFF557862), onTertiary = Color.White,
            tertiaryContainer = Color(0xFFDEEBDF), onTertiaryContainer = Color(0xFF304E39),
            background = Color(if (morning) 0xFFF3F0E9 else 0xFFF1F2EE),
            onBackground = Color(0xFF292E2B), surface = Color(0xFFFCFCF8),
            onSurface = Color(0xFF292E2B), onSurfaceVariant = Color(0xFF6D756E),
            surfaceVariant = Color(0xFFE7E8E0), surfaceDim = Color(0xFFE3E5DD),
            surfaceBright = Color.White, surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFFCFCF8), surfaceContainer = Color(0xFFEAECE5),
            surfaceContainerHigh = Color(0xFFDFE5DB), surfaceContainerHighest = Color(0xFFD7DED3),
            outline = Color(0xFF81897F), outlineVariant = Color(0xFFDCE0D6),
            error = Color(0xFFA34538), onError = Color.White,
            errorContainer = Color(0xFFF7DDD5), onErrorContainer = Color(0xFF6F2921),
        )
    } else {
        darkColorScheme(
            primary = Color(if (night) 0xFFBBC6B4 else 0xFFD1DACA), onPrimary = Color(0xFF263024),
            primaryContainer = Color(0xFF354534), onPrimaryContainer = Color(0xFFDEE7D6),
            secondary = Color(0xFFC4AC90), onSecondary = Color(0xFF33291E),
            secondaryContainer = Color(0xFF453C2F), onSecondaryContainer = Color(0xFFE6D3B8),
            tertiary = Color(0xFFAAC3A4), onTertiary = Color(0xFF20301F),
            tertiaryContainer = Color(0xFF354833), onTertiaryContainer = Color(0xFFD9E9D0),
            background = Color(if (night) 0xFF141713 else 0xFF20241F),
            onBackground = Color(0xFFD8DECE), surface = Color(if (night) 0xFF20251E else 0xFF2A3027),
            onSurface = Color(0xFFD8DECE), onSurfaceVariant = Color(0xFFA6B19F),
            surfaceVariant = Color(0xFF394033), surfaceDim = Color(0xFF141713),
            surfaceBright = Color(0xFF394133), surfaceContainerLowest = Color(0xFF11140F),
            surfaceContainerLow = Color(if (night) 0xFF20251E else 0xFF2A3027),
            surfaceContainer = Color(0xFF272E23), surfaceContainerHigh = Color(0xFF34412F),
            surfaceContainerHighest = Color(0xFF404C39), outline = Color(0xFF89947D),
            outlineVariant = Color(0xFF424C3B), error = Color(0xFFE3A28E), onError = Color(0xFF4C231A),
            errorContainer = Color(0xFF59342A), onErrorContainer = Color(0xFFFFDACE),
        )
    }
}
