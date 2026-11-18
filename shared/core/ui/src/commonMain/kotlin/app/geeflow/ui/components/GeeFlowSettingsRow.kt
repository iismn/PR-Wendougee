package app.geeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GeeFlowSettingsRow(
    title: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val background by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.075f)
            expanded -> MaterialTheme.colorScheme.background
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(200, easing = CoffeeMotionEasing),
        label = "settings-row-selection",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .selectable(selected = selected, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(start = 20.dp, end = if (trailingContent == null) 20.dp else 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
            modifier = Modifier.weight(1f).padding(end = if (trailingContent == null) 0.dp else 8.dp),
        )
        trailingContent?.invoke()
    }
}
