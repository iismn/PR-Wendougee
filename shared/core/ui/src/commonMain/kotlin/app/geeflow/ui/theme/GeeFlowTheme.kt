package app.geeflow.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.geeflow.ui.isWidthExpanded

@Composable
fun GeeFlowTheme(
    darkMode: Boolean,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    phase: CoffeeDayPhase = if (darkMode) CoffeeDayPhase.Night else CoffeeDayPhase.Day,
    content: @Composable () -> Unit,
) {
    @Composable fun tone(color: Color) = animateColorAsState(color, tween(600), label = "appearance").value
    val animatedColors = colorScheme.copy(
        primary = tone(colorScheme.primary), onPrimary = tone(colorScheme.onPrimary),
        background = tone(colorScheme.background), onBackground = tone(colorScheme.onBackground),
        surface = tone(colorScheme.surface), onSurface = tone(colorScheme.onSurface),
        onSurfaceVariant = tone(colorScheme.onSurfaceVariant),
        surfaceContainerLow = tone(colorScheme.surfaceContainerLow),
        surfaceContainer = tone(colorScheme.surfaceContainer),
        surfaceContainerHigh = tone(colorScheme.surfaceContainerHigh),
        surfaceContainerHighest = tone(colorScheme.surfaceContainerHighest),
    )
    val spacing = if (isWidthExpanded()) expandedSpacing() else compactSpacing()
    val customColors = if (darkMode) darkCustomColors else lightCustomColors

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalColors provides customColors,
    ) {
        MaterialTheme(
            colorScheme = animatedColors.copy(background = Color.Transparent),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(32.dp),
            ),
            typography = geeFlowTypography(),
            content = {
                CoffeeBackdrop(phase = phase, modifier = modifier) {
                    Surface(color = Color.Transparent, content = content)
                }
            },
        )
    }
}

@Composable
fun GeeFlowThemePreview(
    darkMode: Boolean,
    content: @Composable () -> Unit,
) {
    GeeFlowTheme(
        darkMode = darkMode,
        colorScheme = coffeeColorScheme(if (darkMode) CoffeeDayPhase.Night else CoffeeDayPhase.Day),
        content = content,
    )
}

object GeeFlowTheme {
    val spacing: GeeFlowSpacing
        @Composable
        get() = LocalSpacing.current

    val colors: Colors
        @Composable
        get() = LocalColors.current
}
