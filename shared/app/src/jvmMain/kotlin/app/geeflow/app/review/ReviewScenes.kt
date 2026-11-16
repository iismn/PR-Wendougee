package app.geeflow.app.review

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.geeflow.presentation.feature.device.dashboard.DeviceDesignReviewContent
import app.geeflow.presentation.feature.device.add.DeviceWarningDesignPreview
import app.geeflow.presentation.feature.device.add.AddDeviceDesignPreview
import app.geeflow.presentation.feature.device.dashboard.freecontrol.FreeControlDesignPreview
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardDesignPreview
import app.geeflow.presentation.feature.device.dashboard.maintenance.MaintenanceReminderDesignPreview
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceDesignPreview
import app.geeflow.presentation.feature.device.list.DeviceListDesignPreview
import app.geeflow.presentation.feature.device.settings.brewing.BrewingSettingsDesignPreview
import app.geeflow.presentation.feature.device.settings.connectivity.ConnectivitySettingsDesignPreview
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsDesignPreview
import app.geeflow.presentation.feature.device.settings.maintenance.MaintenanceSettingsDesignPreview
import app.geeflow.presentation.feature.intro.IntroDesignPreview
import app.geeflow.presentation.feature.user.list.UserListDesignPreview
import app.geeflow.presentation.feature.user.settings.about.AboutDesignPreview
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsDesignPreview
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesDesignPreview
import app.geeflow.presentation.feature.user.settings.main.UserSettingsDesignPreview
import app.geeflow.presentation.feature.user.settings.profile.ProfileSettingsDesignPreview
import app.geeflow.presentation.feature.user.list.add.AddUserDesignPreview
import app.geeflow.presentation.feature.user.settings.about.licenses.LicensesDesignPreview

data class ReviewScene(val id: String, val title: String, val category: String, val content: @Composable () -> Unit)

val reviewScenes = listOf(
    ReviewScene("dashboard", "프로파일 홈", "추출") { DeviceDesignReviewContent("dashboard") },
    ReviewScene("profile-editor", "프로파일 편집", "추출") { DeviceDesignReviewContent("profile-editor") },
    ReviewScene("step-editor", "단계 편집", "추출") { DeviceDesignReviewContent("step-editor") },
    ReviewScene("temperature", "빠른 온도 조절", "추출") { DeviceDesignReviewContent("temperature") },
    ReviewScene("temperature-applying", "빠른 온도 조절 · 적용 중", "추출") { DeviceDesignReviewContent("temperature-applying") },
    ReviewScene("add-device", "기기 검색", "시작·연결") { AddDeviceDesignPreview() },
    ReviewScene("free-control", "자유추출 · 압력", "추출") { FreeControlDesignPreview() },
    ReviewScene("free-control-flow", "자유추출 · 유량", "추출") { FreeControlDesignPreview(app.geeflow.presentation.feature.device.dashboard.freecontrol.ControlMode.Flow) },
    ReviewScene("bluetooth-permission", "블루투스 권한", "팝업") { DeviceDashboardDesignPreview() },
    ReviewScene("cleaning-reminder", "세척 알림", "팝업") { MaintenanceReminderDesignPreview() },
    ReviewScene("quick-cleaning", "빠른 세척", "머신") { ReviewSheet { QuickMaintenanceDesignPreview() } },
    ReviewScene("devices", "기기 목록", "시작·연결") { DeviceListDesignPreview() },
    ReviewScene("boilers", "보일러·추출 설정", "머신") { SettingsPair(machine = true, selected = 0) { BrewingSettingsDesignPreview() } },
    ReviewScene("connectivity", "연결 설정", "머신") { SettingsPair(machine = true, selected = 2) { ConnectivitySettingsDesignPreview() } },
    ReviewScene("machine", "머신 설정", "머신") { DeviceSettingsDesignPreview() },
    ReviewScene("maintenance", "세척·관리 설정", "머신") { SettingsPair(machine = true, selected = 1) { MaintenanceSettingsDesignPreview() } },
    ReviewScene("welcome", "시작", "시작·연결") { IntroDesignPreview() },
    ReviewScene("welcome-profile", "시작 · 프로필 만들기", "시작·연결") { IntroDesignPreview(formShown = true) },
    ReviewScene("users", "사용자 선택", "사용자") { UserListDesignPreview() },
    ReviewScene("about", "앱 정보", "사용자") { SettingsPair(machine = false, selected = 3) { AboutDesignPreview() } },
    ReviewScene("appearance", "화면·테마 설정", "사용자") { SettingsPair(machine = false, selected = 2) { AppearanceSettingsDesignPreview() } },
    ReviewScene("preferences", "추출 환경설정", "사용자") { SettingsPair(machine = false, selected = 1) { BrewingPreferencesDesignPreview() } },
    ReviewScene("settings", "사용자 설정", "사용자") { UserSettingsDesignPreview() },
    ReviewScene("user-profile", "사용자 프로필", "사용자") { SettingsPair(machine = false, selected = 0) { ProfileSettingsDesignPreview() } },
    ReviewScene("add-user", "사용자 추가", "사용자") { ReviewSheet { AddUserDesignPreview() } },
    ReviewScene("licenses", "오픈소스 라이선스", "사용자") { LicensesDesignPreview() },
    ReviewScene("charts", "추출 그래프", "추출") { DeviceDesignReviewContent("charts") },
    ReviewScene("history", "추출 기록", "추출") { DeviceDesignReviewContent("history") },
    ReviewScene("profile-details", "프로파일 이름·설명", "팝업") { DeviceDesignReviewContent("profile-details") },
    ReviewScene("finish-target", "추출 종료 수치", "팝업") { DeviceDesignReviewContent("finish-target") },
    ReviewScene("single-dose", "그라인더 제어", "팝업") { DeviceDesignReviewContent("single-dose") },
    ReviewScene("step-rename", "단계 이름 변경", "팝업") { DeviceDesignReviewContent("step-rename") },
    ReviewScene("step-value", "단계 수치 입력", "팝업") { DeviceDesignReviewContent("step-value") },
    ReviewScene("user-rename", "사용자 이름 변경", "팝업") { ProfileSettingsDesignPreview("user-rename") },
    ReviewScene("user-delete", "사용자 삭제 확인", "팝업") { ProfileSettingsDesignPreview("user-delete") },
    ReviewScene("remove-photo", "사진 삭제 확인", "팝업") { ProfileSettingsDesignPreview("remove-photo") },
    ReviewScene("theme-mode", "시간·화면 모드 선택", "팝업") { AppearanceSettingsDesignPreview("theme-mode") },
    ReviewScene("theme-picker", "앱 테마 선택", "팝업") { AppearanceSettingsDesignPreview("theme-picker") },
    ReviewScene("palette-picker", "색조 선택", "팝업") { AppearanceSettingsDesignPreview("palette-picker") },
    ReviewScene("custom-color", "사용자 색상 선택", "팝업") { AppearanceSettingsDesignPreview("custom-color") },
    ReviewScene("screensaver-timeout", "화면 보호 시간 입력", "팝업") { AppearanceSettingsDesignPreview("screensaver-timeout") },
    ReviewScene("temperature-unit", "온도 단위 선택", "팝업") { BrewingPreferencesDesignPreview("temperature-unit") },
    ReviewScene("restore-profiles", "기본 레시피 복원 확인", "팝업") { BrewingPreferencesDesignPreview("restore-profiles") },
    ReviewScene("device-model", "머신 모델 선택", "팝업") { DeviceListDesignPreview("device-model") },
    ReviewScene("device-rename", "머신 이름 변경", "팝업") { DeviceListDesignPreview("device-rename") },
    ReviewScene("device-warning", "기기 기능 안내", "팝업") { DeviceWarningDesignPreview() },
    ReviewScene("device-found", "기기 검색 결과", "시작·연결") { AddDeviceDesignPreview(found = true) },
    ReviewScene("screensaver", "화면 보호기", "사용자") { app.geeflow.app.screensaver.Screensaver(if (MaterialTheme.colorScheme.surface.luminance() < .1f) "21:41" else "09:41", {}) },
).sortedBy { when (it.category) { "추출" -> 0; "머신" -> 1; "사용자" -> 2; "시작·연결" -> 3; else -> 4 } }

@Composable
private fun ReviewSheet(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = 720.dp)) { content() }
    }
}
