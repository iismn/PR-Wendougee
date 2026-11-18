package app.geeflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.geeflow.ui.GeeFlowInsets
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource

/** A fixed header shares the dashboard's type scale on narrow and wide panes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeeFlowTopBar(
    title: String,
    subtitle: String?,
    navIconClick: () -> Unit,
    navIconPainter: Painter? = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
    navIconContentDescription: String? = stringResource(Res.string.common_go_back),
    windowInsets: WindowInsets = GeeFlowInsets.top,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    titleVerticalPadding: Dp = 20.dp,
) {
    SideEffect {
        scrollBehavior?.state?.let { it.heightOffsetLimit = 0f; it.heightOffset = 0f }
    }
    val frameVerticalPadding = 12.dp
    val textVerticalPadding = (titleVerticalPadding - frameVerticalPadding).coerceAtLeast(0.dp)
    BoxWithConstraints(
        modifier.windowInsetsPadding(windowInsets).padding(horizontal = horizontalPadding, vertical = frameVerticalPadding),
    ) {
        val navigation: @Composable () -> Unit = {
            if (navIconPainter != null) IconButton(onClick = navIconClick) {
                Icon(navIconPainter, navIconContentDescription, Modifier.size(20.dp))
            }
        }
        val heading: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(
                    subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (maxWidth < 260.dp && navIconPainter != null) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                navigation()
                Box(Modifier.padding(horizontal = 12.dp, vertical = textVerticalPadding)) { heading() }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                navigation()
                Box(Modifier.weight(1f).padding(vertical = textVerticalPadding)) { heading() }
            }
        }
    }
}
