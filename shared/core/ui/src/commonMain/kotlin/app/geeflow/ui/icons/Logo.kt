package app.geeflow.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Circular symbol from Wendougee's official brand mark; see docs/brand/README.md. */
const val WendougeeMarkPathData = "M50,0 A50,50 0,1 1,50,100 A50,50 0,1 1,50,0 Z " +
    "M50,23.65 A26.35,26.35 0,1 1,50,76.35 A26.35,26.35 0,1 1,50,23.65 Z " +
    "M50,4.55 A7.25,7.25 0,1 1,50,19.05 A7.25,7.25 0,1 1,50,4.55 Z"

val WendougeeMarkPath = PathParser().parsePathString(WendougeeMarkPathData).toNodes()

val GeeFlowIcon.Logo: ImageVector
    get() = WendougeeLogo

private val WendougeeLogo = ImageVector.Builder(
    name = "Wendougee", defaultWidth = 48.dp, defaultHeight = 48.dp,
    viewportWidth = 100f, viewportHeight = 100f,
).addPath(pathData = WendougeeMarkPath, pathFillType = PathFillType.EvenOdd,
    fill = SolidColor(Color.Black)).build()
