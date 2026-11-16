package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.theme.coffeeGlass
import app.geeflow.ui.theme.coffeeSurface
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_go_back
import org.jetbrains.compose.resources.stringResource

/** Dimensions are taken from ProfileHome: 20/12 margins, 20 gaps, 42 touch targets, 20 icons. */
object CoffeeUi {
    val icon = 20.dp
    val touch = 42.dp
    val gap = 20.dp
    val panelPadding = 20.dp
}

/** Home's list text is 12sp, section headings 14sp, labels 10sp and metric values 36sp. */
@Composable
fun CoffeeWorkspaceTheme(content: @Composable () -> Unit) {
    val type = MaterialTheme.typography
    val heading = type.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp)
    val body = type.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp)
    MaterialTheme(typography = type.copy(
        titleLarge = heading, titleMedium = heading, titleSmall = type.labelMedium,
        bodyLarge = body, bodyMedium = body, labelLarge = type.labelMedium,
    )) { ProvideTextStyle(body) { content() } }
}

@Composable
fun CoffeeMetric(label: String, value: String, unit: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = if (small) 27.sp else 36.sp, lineHeight = if (small) 32.sp else 40.sp,
                fontWeight = FontWeight.Light, maxLines = 1, modifier = Modifier.alignByBaseline())
            if (unit.isNotEmpty()) Text(unit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                modifier = Modifier.padding(start = 6.dp).alignByBaseline())
        }
    }
}

@Composable
fun CoffeeAction(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick, Modifier.size(CoffeeUi.touch), enabled = enabled) {
        Icon(icon, label, Modifier.size(CoffeeUi.icon))
    }
}

@Composable
fun Modifier.coffeeTintedPanel(): Modifier {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .1f
    val colors = if (dark) listOf(Color(0xB54D719D), Color(0x80334767), Color(0xB5162848))
        else listOf(Color(0xB57498A8), Color(0x806D8D9D), Color(0x4DC8DADD))
    return coffeeGlass(MaterialTheme.shapes.medium, prominent = true)
        .background(Brush.linearGradient(colors), MaterialTheme.shapes.medium)
}

/** Three-column iPad workspace, sharing the home header, metric strip and panel alignment. */
@Composable
fun CoffeeWorkspace(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    metrics: @Composable RowScope.() -> Unit,
    left: (@Composable ColumnScope.() -> Unit)? = null,
    right: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = CoffeeWorkspaceTheme {
    BoxWithConstraints(Modifier.fillMaxSize().geeFlowInsetsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
        val leftWidth = (maxWidth * .18f).coerceIn(166.dp, 220.dp)
        val rightWidth = (maxWidth * .255f).coerceIn(250.dp, 320.dp)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoffeeAction(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.common_go_back), onBack)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                actions()
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().padding(start = if (left != null) leftWidth + 24.dp else 0.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, content = metrics)
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(CoffeeUi.gap)) {
                if (left != null) Column(Modifier.width(leftWidth).fillMaxHeight()
                    .coffeeSurface(MaterialTheme.shapes.medium).padding(8.dp), content = left)
                Column(Modifier.weight(1f).fillMaxHeight(), content = content)
                if (right != null) Column(Modifier.width(rightWidth).fillMaxHeight()
                    .coffeeTintedPanel().padding(CoffeeUi.panelPadding), content = right)
            }
        }
    }
}
