package app.geeflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import geeflow.shared.core.ui.generated.resources.Pretendard_Regular
import geeflow.shared.core.ui.generated.resources.Pretendard_Medium
import geeflow.shared.core.ui.generated.resources.Pretendard_SemiBold
import geeflow.shared.core.ui.generated.resources.Pretendard_Bold
import geeflow.shared.core.ui.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
internal fun geeFlowTypography(): Typography {
    val font = FontFamily(
        Font(Res.font.Pretendard_Regular, FontWeight.Normal),
        Font(Res.font.Pretendard_Medium, FontWeight.Medium),
        Font(Res.font.Pretendard_SemiBold, FontWeight.SemiBold),
        Font(Res.font.Pretendard_Bold, FontWeight.Bold),
    )

    fun style(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = font, fontSize = size.sp, lineHeight = height.sp,
        fontWeight = weight, letterSpacing = (-0.2).sp, fontFeatureSettings = "tnum",
    )
    return Typography(
        displayLarge = style(52, 60), displayMedium = style(42, 50), displaySmall = style(34, 42),
        headlineLarge = style(30, 38), headlineMedium = style(26, 34), headlineSmall = style(22, 30),
        titleLarge = style(22, 30, FontWeight.Medium), titleMedium = style(18, 26, FontWeight.Medium),
        titleSmall = style(16, 24, FontWeight.Medium), bodyLarge = style(16, 24),
        bodyMedium = style(14, 21), bodySmall = style(12, 18),
        labelLarge = style(14, 20, FontWeight.Medium), labelMedium = style(12, 18, FontWeight.Medium),
        labelSmall = style(10, 15, FontWeight.Medium),
    )
}
