# Mac에서 이어서 작업하기

현재 앱 기반은 **GeeFlow(Kotlin Multiplatform/Compose)**입니다. 이전 Crema Xcode 프로젝트와 웹 UI는 제거했습니다. 기존 작업 이력은 Git 기록에 남아 있습니다.

```sh
git clone https://github.com/iismn/PR-Wendougee.git
cd PR-Wendougee
```

JDK21과 최신 Xcode를 설치합니다. Android 타깃도 포함하므로 Android SDK는 Android Studio에서 설치하고, 필요하면 `local.properties`에 `sdk.dir`를 지정합니다. 프로젝트의 Gradle wrapper를 사용하세요.

```sh
./gradlew :desktopApp:run
./gradlew :shared:feature:device:dashboard:generateStoreScreenshots
open iosApp/iosApp.xcodeproj
```

Xcode의 `iosApp` 스킴에서 iPad Simulator를 선택합니다. 실제 iPad 설치는 본인 Signing Team과 고유 Bundle Identifier를 설정합니다. 배포나 앱스토어 제출은 수행하지 않았습니다.

검증 명령:

```sh
./gradlew :shared:core:ui:jvmTest :shared:feature:device:dashboard:jvmTest :shared:domain:user:jvmTest
./gradlew :desktopApp:compileKotlin
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator build CODE_SIGNING_ALLOWED=NO
```

`GeeFlow iPad UI` Actions가 같은 코드의 실제 Compose 캡처, 관련 테스트, 데스크톱 컴파일 및 iPad 시뮬레이터 빌드를 검사합니다. 캡처는 `geeflow-compose-review` 아티팩트에서 받을 수 있습니다.

실제 iPad에서 가로 안전 영역·큰 글자·터치·회전 및 LITA 연결·온도·프로필 전송·추출·정지·저울·기록을 확인해야 합니다. 이번 변경은 UI와 appearance 설정이며 머신 프로토콜은 GeeFlow 원본을 따릅니다.
