package app.geeflow.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Shared translucent material. No blur is applied to chart lines, labels or touch targets. */
@Composable
fun Modifier.coffeeGlass(shape: Shape = MaterialTheme.shapes.medium): Modifier {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.1f
    val tint = if (dark) Color(0xFF0C1421) else Color.White
    val edge = if (dark) Color(0xFFB9D6FF) else Color.White
    return clip(shape)
        .background(Brush.linearGradient(listOf(
            tint.copy(alpha = if (dark) 0.70f else 0.76f),
            tint.copy(alpha = if (dark) 0.48f else 0.50f),
        )))
        .border(1.dp, Brush.linearGradient(listOf(
            edge.copy(alpha = if (dark) 0.22f else 0.94f),
            edge.copy(alpha = if (dark) 0.04f else 0.32f),
            edge.copy(alpha = if (dark) 0.12f else 0.66f),
        )), shape)
}
