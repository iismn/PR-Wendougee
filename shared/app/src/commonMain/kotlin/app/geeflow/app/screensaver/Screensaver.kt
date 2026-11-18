package app.geeflow.app.screensaver

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.platform.rememberFormattedTime
import app.geeflow.ui.components.DotMatrixClock
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview

@Composable
internal fun Screensaver(onDismiss: () -> Unit, modifier: Modifier = Modifier, sizePercent: Int = 100) =
    Screensaver(rememberFormattedTime(), onDismiss, modifier, sizePercent)

@Composable
internal fun Screensaver(formattedTime: String, onDismiss: () -> Unit, modifier: Modifier = Modifier, sizePercent: Int = 100) {
    val time = remember(formattedTime) { parseScreensaverTime(formattedTime) }
    val dismiss by rememberUpdatedState(onDismiss)
    BoxWithConstraints(
        modifier.fillMaxSize().testTag("dot-screensaver").background(Color.Black).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).consume()
                val up = waitForUpOrCancellation(PointerEventPass.Initial)
                up?.consume()
                if (up != null) dismiss()
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        val baseWidth = minOf(maxWidth * .324f, maxHeight * .87f, 336.dp)
        val clockWidth = minOf(baseWidth * (sizePercent.coerceIn(60, 200) / 100f),
            (maxWidth - 104.dp).coerceAtLeast(1.dp), maxHeight * 2f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.widthIn(min = 28.dp).testTag("clock-period"),
                horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.1.sp, lineHeight = 11.7.sp)
                Text("AM", style = labelStyle, color = Color.White.copy(alpha = if (time.isAm) .8f else .2f))
                Text("PM", style = labelStyle, color = Color.White.copy(alpha = if (time.isAm) .2f else .8f))
            }
            Spacer(Modifier.width(24.dp).testTag("clock-period-gap"))
            DotMatrixClock(time.digits, formattedTime, Modifier.width(clockWidth).testTag("clock-digits"))
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() = Screensaver("09:41", {})
