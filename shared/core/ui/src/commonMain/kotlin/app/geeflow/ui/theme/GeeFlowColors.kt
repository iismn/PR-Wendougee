package app.geeflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val waterLight = Color(0xFF396CC7)
private val waterVariantLight = Color(0xFF367C9E)

private val waterDark = Color(0xFF80A7F2)
private val waterVariantDark = Color(0xFF7EC6E7)

@Immutable
data class Colors(
    val water: Color,
    val waterVariant: Color,
)

val lightCustomColors = Colors(
    water = waterLight,
    waterVariant = waterVariantLight,
)

val darkCustomColors = Colors(
    water = waterDark,
    waterVariant = waterVariantDark,
)

val LocalColors = staticCompositionLocalOf { lightCustomColors }

fun Color.disabled(alpha: Float = 0.38f): Color = this.copy(alpha = alpha)
