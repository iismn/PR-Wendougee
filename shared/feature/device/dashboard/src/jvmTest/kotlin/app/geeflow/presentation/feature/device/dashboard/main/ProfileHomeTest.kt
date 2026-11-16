package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.*
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProfileHomeTest {
    @Test
    fun orbitSelectionAndEditingUseExistingEventsWithoutStartingTheMachine() = runComposeUiTest {
        val profileEvents = mutableListOf<ProfileListEvent>()
        val deviceEvents = mutableListOf<DeviceDashboardEvent>()
        val profiles = ProfileListViewState(profiles = listOf(
            ProfileListViewState.Profile("a", "1", "Orbit A", "36g", true, selected = true),
            ProfileListViewState.Profile("b", "2", "Orbit B", "42g", true),
        ))
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), profiles, SnackbarHostState(), deviceEvents::add, profileEvents::add, {})
        } }
        onNodeWithTag("profile-orbit-b").performClick()
        onNodeWithTag("profile-orbit-selected").performClick()
        runOnIdle {
            assertEquals(listOf(ProfileListEvent.ProfileSelected("b"), ProfileListEvent.EditProfileClicked("a")), profileEvents)
            assertTrue(deviceEvents.isEmpty())
        }
    }

    @Test
    fun emptyLibraryDoesNotInventASelectedProfile() = runComposeUiTest {
        val events = mutableListOf<ProfileListEvent>()
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), ProfileListViewState(), SnackbarHostState(), {}, events::add, {})
        } }
        onNodeWithTag("profile-orbit-selected").assertIsNotEnabled()
        runOnIdle { assertTrue(events.isEmpty()) }
    }
}
