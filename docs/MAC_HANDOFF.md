# Mac에서 이어서 작업하기

이 앱은 GeeFlow의 Kotlin Multiplatform/Compose 기반 UI 포크입니다. iPad 가로 홈은 원형 프로파일, 왼쪽 목록, 오른쪽 추출 그래프와 중앙 하단의 통합 조작부로 구성됩니다.

## 로컬 iOS 실행

```sh
git clone https://github.com/iismn/PR-Wendougee.git
cd PR-Wendougee
open iosApp/iosApp.xcodeproj
```

Xcode 26 이상과 JDK 21을 설치합니다. Xcode에 맞는 macOS 버전은 [Apple 요구 사항](https://developer.apple.com/xcode/system-requirements/)을 확인하세요. Android 타깃도 포함하므로 Android Studio의 SDK Manager에서 Android SDK를 설치하고, 필요하면 저장소 루트의 `local.properties`에 `sdk.dir=/Users/사용자/Library/Android/sdk`를 지정합니다. `local.properties`는 커밋하지 않습니다.

Xcode의 **iosApp** 스킴에서 iPad Simulator를 선택하고 Run을 누릅니다. 실제 iPad 설치에는 본인 Signing Team과 고유 Bundle Identifier를 설정합니다. 이 포크의 설치용 IPA나 App Store 앱은 아직 배포하지 않았습니다.

Windows에서는 Xcode/iOS 빌드를 할 수 없습니다. 이 저장소의 CI는 임시 Mac에서 Kotlin/Native 프레임워크와 iOS 앱을 함께 빌드하므로 첫 빌드가 오래 걸립니다. 로컬 Mac에서는 같은 작업 폴더, Gradle 캐시와 Xcode DerivedData를 유지하고 `clean`을 반복하지 마세요. UI 수정은 아래 JVM 렌더로 먼저 확인하고, UIKit 재질과 터치는 iPad에서 확인하면 됩니다.

## 빠른 UI 검토

```sh
./gradlew :desktopApp:run
./gradlew :shared:feature:device:dashboard:generateStoreScreenshots
node scripts/preview-compose.cjs docs/screenshots
```

`http://127.0.0.1:4181`에서 홈·온도·레시피·단계 화면의 낮과 밤을 비교합니다. 파일이 바뀌면 자동으로 갱신됩니다. 이 창은 실제 Compose 화면을 렌더링한 이미지 뷰어이며, 머신을 제어하는 앱은 아닙니다. JVM 이미지는 UIKit Liquid Glass의 증거가 아닙니다.

저장된 검토 이미지를 보려면 `node scripts/preview-compose.cjs docs/review`를 사용합니다. 소스 커밋과 렌더 환경은 `docs/review/latest.json`에 기록합니다.

## 한국어와 시간 연동

한국어 iPad에서는 UI가 자동으로 한국어로 표시됩니다. 다른 언어의 iPad에서는 설정 앱의 GeeFlow 언어를 한국어로 선택하세요. 앱의 **화면 → 언어**에서도 시스템 설정으로 이동할 수 있습니다. Pretendard 네 가지 굵기는 앱에 포함됩니다.

**화면 → 화면 모드 → 시간에 맞춰 자동**을 선택하면 현지 06:00/12:00/18:00/23:00에 맞춰 색상이 바뀝니다. 앱이 다시 활성화될 때와 실행 중 30초마다 시간대를 확인합니다. 앱 색상만 바꾸며 디스플레이 밝기는 바꾸지 않습니다.

## 네이티브 Liquid Glass

`iosApp/iosApp/NativeGlassViewController.swift`가 UIKit 배경, `UIVisualEffectView`와 `UIGlassEffect`, 투명 Compose 전경을 관리합니다. iPadOS 26 이상은 실제 `UIGlassEffect`, 이전 버전은 `UIBlurEffect(.systemMaterial)`를 사용합니다. Compose 컴포넌트가 전달하는 창 좌표로 네이티브 재질의 위치를 맞추고, 제거와 터치 상태도 전달합니다.

빠른 설정과 전체 추출 설정에는 같은 추출·스팀 보일러 곡선 눈금 카드를 사용합니다. 머신에서 제공한 값으로 조절되고 기존 확인·저장 동작으로 적용됩니다.

Xcode의 **NativeReview** 스킴에서 iPad 시뮬레이터를 선택하고 Test를 실행하면 XCTest가 기기를 가로로 돌려 낮·밤 홈과 온도 화면을 캡처합니다. CI와 같은 파일을 추출하려면 다음 명령을 실행합니다.

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme NativeReview -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath build/ios-review build-for-testing CODE_SIGNING_ALLOWED=NO
bash scripts/capture-native-ipad.sh
```

`build/native-review`에는 PNG와 런타임 재질 객체 JSON, 소스 커밋이 저장됩니다. 스크립트는 iPadOS 26 이상, 가로 이미지 크기, 실제 `UIGlassEffect` 클래스 존재를 검사합니다. 캡처 진입점은 Debug 전용이며 GeeFlow의 데모 데이터로 실제 프로덕션 UI를 구성합니다.

## 검증

```sh
./gradlew :shared:core:ui:jvmTest :shared:feature:device:dashboard:jvmTest :shared:domain:user:jvmTest :shared:domain:device:jvmTest
./gradlew :desktopApp:compileKotlin
```

관련 JVM 테스트 240개와 한국어 리소스 419개 검사가 통과했습니다. 프로파일 선택·편집, 온도 범위와 비활성 상태, 기기 제공 값 선택, 네이티브 재질의 좌표·제거·터치 전달을 포함합니다. 최신 iOS 빌드와 캡처 결과는 [Actions](https://github.com/iismn/PR-Wendougee/actions/workflows/ipad-design.yml)에서 확인하세요.

실제 iPad의 가로 안전 영역·큰 글자·터치·회전과 LITA의 연결·온도 적용·프로파일 전송·추출·정지·저울·기록은 하드웨어에서 확인해야 합니다. 머신 프로토콜은 GeeFlow 원본을 따릅니다.
