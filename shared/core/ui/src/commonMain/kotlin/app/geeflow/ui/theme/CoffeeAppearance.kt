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
