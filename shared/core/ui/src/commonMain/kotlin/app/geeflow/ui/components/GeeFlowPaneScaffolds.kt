@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)

package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.geeflow.ui.GeeFlowInsets
import app.geeflow.ui.theme.GeeFlowTheme

/** RootNavigation uses the default strategy which only takes over for multi-pane scenes. */
@Composable
fun isListDetailExpanded(): Boolean = LocalListDetailSceneScope.current != null

/** The header stays above the list even when navigation displays a detail beside it. */
@Composable
fun GeeFlowListScaffold(
    title: String,
    subtitle: String? = null,
    navIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    expanded: Boolean = isListDetailExpanded(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollModifier = if (scrollBehavior != null) {
        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    } else {
        Modifier
    }
    Row(modifier.then(scrollModifier).fillMaxSize()) {
        GeeFlowScaffoldFrame(
            modifier = Modifier.weight(1f),
            contentWindowInsets = if (expanded) GeeFlowInsets.startPane else GeeFlowInsets.content,
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background,
            horizontalWave = !expanded,
            header = {
                GeeFlowTopBar(
                    title = title,
                    subtitle = subtitle,
                    navIconClick = navIconClick,
                    navIconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            content = content,
        )
    }
}

/** A first row with its own inset still starts the same distance from the divider. */
@Composable
fun settingsDetailVerticalPadding(firstItemPadding: Dp): PaddingValues {
    val gap = GeeFlowTheme.spacing.contentVertical
    return PaddingValues(
        top = if (isListDetailExpanded()) (gap - firstItemPadding).coerceAtLeast(0.dp) else gap,
        bottom = gap,
    )
}

/** Standalone detail has a back header; an embedded detail owns only its outer edges. */
@Composable
fun GeeFlowDetailScaffold(
    title: String,
    subtitle: String? = null,
    navIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    expanded: Boolean = isListDetailExpanded(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    GeeFlowScaffoldFrame(
        modifier = if (!expanded && scrollBehavior != null) {
            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            modifier
        },
        contentWindowInsets = if (expanded) GeeFlowInsets.endPane else GeeFlowInsets.content,
        header = {
            val horizontalPadding = if (expanded) GeeFlowTheme.spacing.contentHorizontal else 12.dp
            val headerFill = MaterialTheme.colorScheme.surfaceContainerLow
            Column(Modifier.fillMaxWidth().then(if (expanded) Modifier.background(headerFill) else Modifier)) {
                GeeFlowTopBar(
                    title = title,
                    subtitle = subtitle,
                    navIconClick = navIconClick,
                    navIconPainter = if (expanded) null else rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    horizontalPadding = horizontalPadding,
                    titleVerticalPadding = if (expanded) GeeFlowTheme.spacing.contentVertical else 20.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (expanded) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f),
                    )
                }
            }
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        content = { padding ->
            if (expanded) {
                val headerFill = MaterialTheme.colorScheme.surfaceContainerLow
                Box(
                    Modifier.fillMaxSize().padding(padding).clipToBounds().drawWithCache {
                        val fadeHeight = minOf(28.dp.toPx(), size.height).coerceAtLeast(1f)
                        val edgeFade = Brush.verticalGradient(
                            0f to headerFill,
                            .26f to headerFill.copy(alpha = .87f),
                            .72f to headerFill.copy(alpha = .18f),
                            1f to headerFill.copy(alpha = 0f),
                            endY = fadeHeight,
                        )
                        onDrawWithContent {
                            drawContent()
                            // The fade covers the scrolling pixels at the top of the viewport.
                            drawRect(edgeFade, size = Size(size.width, fadeHeight))
                        }
                    },
                ) { content(PaddingValues()) }
            } else {
                content(padding)
            }
        },
    )
}
