# Mac에서 이어서 작업하기

저장소: [iismn/PR-Wendougee](https://github.com/iismn/PR-Wendougee)

## 열기

macOS 15 이상과 Xcode 16 이상(Swift 6)이 필요합니다. 저장된 Xcode 프로젝트와 두 공유 스킴을 바로 사용할 수 있습니다.

```sh
git clone https://github.com/iismn/PR-Wendougee.git
cd PR-Wendougee
open Crema.xcodeproj
```

Xcode에서 `Crema-macOS → My Mac` 또는 `Crema-iOS → iPhone/iPad Simulator`를 선택합니다. 실제 iPhone/iPad에 설치할 때만 iOS 타깃의 **Signing & Capabilities → Team**을 본인 팀으로 설정하고 Bluetooth 권한을 허용합니다. 프로젝트를 재생성할 때는 `project.yml`의 해당 설정도 함께 맞춥니다.

## 빌드 확인

저장소 루트에서 실행합니다. 아래 명령과 CI는 인증서·프로비저닝 없이 빌드합니다.

```sh
swift test
xcodebuild -list -project Crema.xcodeproj

xcodebuild build -project Crema.xcodeproj -scheme Crema-iOS \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath .build/ios-app CODE_SIGNING_ALLOWED=NO

xcodebuild build -project Crema.xcodeproj -scheme Crema-macOS \
  -configuration Debug -destination 'platform=macOS' \
  -derivedDataPath .build/macos-app CODE_SIGNING_ALLOWED=NO
```

`.github/workflows/apple-build.yml`도 같은 테스트와 두 앱 빌드를 실행합니다. `macos-15`의 Xcode 16.4를 지정했고, checkout은 [공식 v5.1.0 커밋](https://github.com/actions/checkout/commit/fbc6f3992d24b796d5a048ff273f7fcc4a7b6c09)에 고정했습니다. 배포 단계나 추가 secrets는 없습니다. 설치 도구는 [공식 runner 이미지 목록](https://github.com/actions/runner-images/blob/main/images/macos/macos-15-arm64-Readme.md)에서 확인할 수 있습니다.

## 현재 UI와 확인할 항목

- 주 사용 환경은 **가로로 거치한 iPad**입니다. 820pt 이상에서는 상단 정보와 도구를 한 줄에 정리하고, 차트·네 측정값·단계·조작부 높이를 가용 창 높이에 맞춥니다.
- GeeFlow의 공개 태블릿 화면을 참고해 1000pt 이상 가로 화면에는 실제 프로필 목록을 오른쪽에 상시 표시합니다. 선택 시 재생을 멈추고 해당 레시피의 Live 목표 그래프로 전환합니다. 자동 연결/추출은 하지 않습니다. 추출 중 및 완료 결과 확인 중에는 빠른 선택을 잠급니다. 큰 글자 접근성 크기에서는 기존 세로 구성으로 돌아갑니다.
- 가로 편집 화면은 왼쪽의 노드 그래프와 오른쪽의 단계 선택·다이얼·눈금자가 함께 보입니다. 기본 레시피와 그라인더 항목은 왼쪽 그래프 아래에 있습니다.
- 메인 순서는 기존 앱의 **헤더 → 추출 차트 → 측정값 → 단계 타임라인 → 추출/재생 조작부**입니다.
- 프로필과 추출 기록은 기존 세로 목록과 시트로 열립니다. 프로필 선택·생성·편집·공유·커뮤니티·설정, 기록의 수율·시간·맛·메모·삭제 경로를 유지했습니다.
- Pretendard Regular/Medium/SemiBold/Bold를 iOS·macOS에 포함하고 등록했습니다. 글자 누락과 시스템 폰트로 대체되는 곳을 실제 앱에서 확인합니다.
- 로컬 시간에 따라 아침(06–12)·낮(12–18)·저녁(18–23)·밤(23–06) 색상이 바뀝니다. 수동 선택과 Auto 복귀, 시간대 변경도 확인합니다.
- 측정값의 점 숫자·단위 기준선, 다이얼 중심과 고정 마커, 눈금자의 중앙 마커를 확인합니다.
- 그래프의 번호와 하단 단계 타임라인을 비교합니다. 좁은 단계의 번호는 겹치지 않도록 생략하며, 단계 경계는 그대로 표시합니다.
- 기존 편집기의 압력/유량 다이얼과 시간/대기 눈금자는 실제 단계 값에 연결됩니다. 드래그·스냅·화살표/±·Home/End·VoiceOver와 동작 줄이기를 확인합니다.

## 파일 위치

| 내용 | 위치 |
| --- | --- |
| 앱 진입·시간 갱신 | `Sources/CremaApp/CremaApp.swift` |
| 메인 화면 | `Sources/CremaApp/Views/LiveShotView.swift` |
| 목록·기록·편집기 | `Sources/CremaApp/Views/ProfilesView.swift`, `ShotHistoryView.swift`, `ProfileEditView.swift` |
| 측정값·차트·추출 조작 | `MetricHUD.swift`, `ShotChart.swift`, `BrewActionZone.swift` (위 Views 폴더) |
| 다이얼·눈금자 | `Sources/CremaApp/Views/ProfileNodeEditor.swift` |
| 색상·시간·점 숫자 | `Sources/CremaApp/Theme/CremaTheme.swift` |
| 글꼴 도우미·폰트·라이선스 | `Sources/CremaApp/Theme/CremaTypography.swift`, `Sources/CremaApp/Resources/Fonts/` |
| 오프라인 디자인 미리보기 | `outputs/ui-preview/index.html` |
| 앱 프로젝트 원본 | `project.yml` |
| 재사용 엔진·테스트 | `Sources/CremaKit/`, `Tests/CremaKitTests/` |

## 남은 실제 기기 확인

Windows에서는 Xcode/SwiftUI 네이티브 실행을 검증하지 못했습니다. HTML 미리보기는 네이티브 렌더링 결과가 아닙니다. Actions의 Apple 빌드와 별도로, `landscape-preview`가 1194×834(낮/밤), 1024×768, 390×844의 HTML 배치 및 편집 조작을 검사하고 `ipad-landscape-preview` 스크린샷을 남깁니다. 실제 iPad에서는 안전 영역·가로 회전·손가락 조작·큰 글자 설정을 확인해야 합니다.

실제 LITA의 BLE 검색·연결·프로필 전송·추출·중단·추출 후 기록 저장은 실기기 확인이 남아 있습니다. 자동 튜닝의 실제 BLE 저울 연동은 기존 코드에도 없으며, 시뮬레이터는 StubScale, 실제 기기는 ManualScale을 사용합니다. UI 작업에서 `Data/`와 `CremaKit/`의 전송·추출 로직은 바꾸지 않았습니다.

Apple CI에서 기존 공유 API의 ISO8601 날짜 처리 실패를 발견해 소수 초가 있는 날짜와 없는 날짜를 모두 읽도록 보완했습니다. 관련 회귀 테스트를 추가했고, 실제 CI 결과는 저장소 Actions에서 확인할 수 있습니다.
