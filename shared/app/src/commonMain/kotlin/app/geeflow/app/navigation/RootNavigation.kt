package app.geeflow.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.savedstate.serialization.SavedStateConfiguration
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.NavFeature
import app.geeflow.ui.animations.BackwardTransition
import app.geeflow.ui.animations.ForwardTransition
import app.geeflow.ui.animations.NavigationEnterMillis
import app.geeflow.ui.animations.NavigationExitMillis
import app.geeflow.ui.animations.NavigationFadeEasing
import app.geeflow.ui.animations.predictiveBackTransition
import app.geeflow.ui.components.EdgeSwipeBack
import app.geeflow.ui.theme.NativeMaterialFade
import kotlinx.serialization.modules.plus
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun RootNavigation(closeApp: () -> Unit) {
    val getInitialDestinationUseCase = koinInject<GetInitialDestinationUseCase>()
    val navFeatures: List<NavFeature> = getKoin().getAll<NavFeature>()
    val initialDestination = remember { getInitialDestinationUseCase() }
    val backStack = rememberNavBackStack(navFeatures.savedStateConfig(), initialDestination)
    val navigator = remember { AppNavigator(finish = { closeApp() }, backStack = backStack) }
    val dialogSceneStrategy = remember { CoffeeDialogSceneStrategy<NavKey>() }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val materialDecorator = remember {
        NavEntryDecorator<NavKey>(onPop = {}, decorate = { entry ->
            NativeMaterialFade(
                LocalNavAnimatedContentScope.current,
                NavigationEnterMillis,
                NavigationExitMillis,
                fadeEasing = NavigationFadeEasing,
            ) { entry.Content() }
        })
    }

    NavigationEdgeSwipeBack(
        enabled = backStack.size > 1,
        onBack = { navigator.navigate(NavEvent.Back) },
        modifier = Modifier.fillMaxSize(),
    ) {
        NavDisplay(
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                materialDecorator,
            ),
            onBack = {
                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            },
            sceneStrategies = listOf(dialogSceneStrategy, listDetailStrategy),
            backStack = backStack,
            transitionSpec = { ForwardTransition },
            popTransitionSpec = { BackwardTransition },
            predictivePopTransitionSpec = { edge -> predictiveBackTransition(edge == NavigationEvent.EDGE_RIGHT) },
            entryProvider = entryProvider {
                navFeatures.forEach {
                    with(it) { provideEntries(navigator) }
                }
            },
        )
    }
}

@Composable
internal fun NavigationEdgeSwipeBack(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val input = remember(dispatcher) { dispatcher?.let { DirectNavigationEventInput() } }
    if (dispatcher != null && input != null) {
        DisposableEffect(dispatcher, input) {
            dispatcher.addInput(input)
            onDispose { dispatcher.removeInput(input) }
        }
    }

    // The native system edge gesture already reaches this dispatcher. Compose drags
    // starting slightly inside the edge (and pointer drags) drive the same transition.
    EdgeSwipeBack(
        enabled = enabled,
        onBack = { if (input == null) onBack() else input.backCompleted() },
        modifier = modifier,
        onSwipeStarted = { input?.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT)) },
        onSwipeProgress = { progress ->
            input?.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = progress))
        },
        onSwipeCancelled = { input?.backCancelled() },
        content = content,
    )
}

private fun List<NavFeature>.savedStateConfig() = SavedStateConfiguration {
    serializersModule = this@savedStateConfig
        .map { it.serializerModule }
        .reduce { acc, module -> acc + module }
}
