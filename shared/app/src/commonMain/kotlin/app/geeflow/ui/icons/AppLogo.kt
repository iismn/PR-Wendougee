package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GeeFlowIcon.AppLogo: ImageVector
    get() = WendougeeAppLogo

private val WendougeeAppLogo = ImageVector.Builder(
    name = "WendougeeApp", defaultWidth = 256.dp, defaultHeight = 256.dp,
    viewportWidth = 160f, viewportHeight = 160f,
).apply {
    path(fill = SolidColor(Color(0xFF101A2B))) {
        moveTo(0f, 0f); lineTo(160f, 0f); lineTo(160f, 160f); lineTo(0f, 160f); close()
    }
    group(translationX = 30f, translationY = 30f) {
        addPath(pathData = WendougeeMarkPath, pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.White))
    }
}.build()
