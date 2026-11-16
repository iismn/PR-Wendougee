package app.geeflow.ui.components

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeeFlowDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val capture = LocalDialogCapture.current
    if (capture != null) {
        CaptureDialog(capture) { GeeFlowDialogSurface(modifier, content) }
        return
    }
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        content = {
            GeeFlowDialogSurface(Modifier, content)
        },
    )
}

@Composable
private fun GeeFlowDialogSurface(modifier: Modifier, content: @Composable () -> Unit) {
    CoffeeWorkspaceTheme { Surface(
        modifier = modifier.widthIn(max = 480.dp).wrapContentWidth().wrapContentHeight(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        content = content,
    ) }
}
