# Woorisseum

Android와 iOS의 UI·비즈니스 로직을 공유하는 Kotlin Multiplatform / Compose Multiplatform 프로젝트입니다.

## 모듈 구조

| 모듈 | 책임 |
| --- | --- |
| `androidApp` | Android 실행 진입점과 앱 설정 |
| `iosApp` | Xcode 프로젝트, 공통 Compose 화면을 표시하는 SwiftUI 호스트 |
| `shared` | 공통 `App()`, iOS `MainViewController()`, 앱 전체 내비게이션·DI 조립 |
| `core:domain` | 기능별 비즈니스 모델, Repository 인터페이스, 필요한 UseCase |
| `core:data` | Repository 구현과 데이터 소스. Firebase·로컬 DB는 추후 추가 |
| `core:designsystem` | 공통 Compose 테마와 재사용 UI |
| `feature:auth` | 로그인·회원가입 화면과 ViewModel |
| `feature:household` | 공동 가계부 생성·참여 화면과 ViewModel |
| `feature:transaction` | 거래 목록·등록·수정 화면과 ViewModel |
| `feature:budget` | 월별 예산·통계 화면과 ViewModel |
| `feature:shopping` | 공유 장보기 화면과 ViewModel |

기능 모듈은 빌드 설정만 준비된 상태이며 실제 화면은 아직 구현하지 않았습니다. 기존 예제 화면은 `shared`에 유지하고, 공통 테마는 `core:designsystem`에서 사용합니다.

## 의존 방향

```text
androidApp / iosApp → shared → feature:* → core:domain
                         │        └────→ core:designsystem
                         ├──→ core:data → core:domain
                         └──→ core:domain / core:designsystem
```

- 기능 모듈은 서로 직접 의존하지 않고, 기능 간 이동은 `shared`에서 연결합니다.
- UI는 domain의 Repository 인터페이스를 사용하고, data 구현 연결은 shared의 DI 조립부가 맡습니다.
- domain에는 Compose, DI, Firebase 의존성을 추가하지 않습니다.
- domain/data 내부에서는 `auth`, `household`, `transaction`, `budget`, `shopping` 패키지로 기능을 구분합니다. 실제 코드가 생길 때 패키지를 추가합니다.
- 기본은 `implementation`입니다. 외부에 공개하는 타입에 필요한 의존성만 `api`로 노출합니다. domain의 Flow·날짜 타입을 위해 Coroutines·Datetime은 `api`로 선언했습니다.
- 모든 공통 모듈은 Android·iOS arm64·iOS Simulator arm64를 지원합니다. iOS용 프레임워크는 `shared`에서만 생성하며, 내부 모듈을 Swift API로 별도 export하지 않습니다.

## 공통 빌드 설정과 라이브러리

버전과 Maven 좌표는 `gradle/libs.versions.toml`에서 관리합니다. Kotlin·CMP·AGP·Gradle·SDK와 Material3 버전은 생성 시 설정을 유지했습니다. Material3는 기존 `1.12.0-alpha03`이므로 안정 버전으로 변경한 상태가 아닙니다.

`build-logic`의 convention plugin으로 반복 설정을 공유합니다.

| 플러그인 | 공통 설정 |
| --- | --- |
| `woorisseum.kmp.library` | KMP, Android KMP library, iOS 타깃, Android SDK·namespace·JVM 11 바이트코드, Kotlin Test·Coroutines Test |
| `woorisseum.compose.library` | KMP 설정 + Compose compiler·UI·Material3·리소스·미리보기 |
| `woorisseum.compose.feature` | Compose 설정 + domain·designsystem, ViewModel·Lifecycle·Koin, Coroutines·Serialization |

추가 라이브러리:

- Coroutines `1.11.0`: 비동기 작업·Flow 및 공통 테스트
- Serialization JSON `1.12.0`: DTO 및 타입 안전한 화면 경로 직렬화
- Datetime `0.8.0`: 공통 날짜·시간대 처리
- Koin `4.2.2`: 공통 DI 및 Compose ViewModel 주입. 초기화·실제 binding은 기능 구현 시 shared에서 추가
- JetBrains Navigation Compose `2.9.2`: Android·iOS 공통 화면 이동. 내비게이션 그래프는 기능 구현 시 추가
- Lifecycle `2.11.0`: 기존 공통 ViewModel·Lifecycle 버전 유지

Firebase, 네트워크 클라이언트, 로컬 DB는 이번 단계에 추가하지 않았습니다. Android 전용 DI·Navigation 라이브러리를 `commonMain`에 넣지 않습니다.

## 개발 환경과 검증

Gradle 실행 JVM은 `gradle/gradle-daemon-jvm.properties`의 JDK 21을 사용합니다. JVM 바이트코드 타깃 11과 Gradle 실행 JDK는 서로 다른 설정입니다. iOS는 Xcode와 Simulator가 필요하며 기존 최소 OS는 iOS 18.2입니다.

```bash
# 모듈·빌드 스크립트 확인
./gradlew projects

# Android 앱 빌드와 공통 테스트(Android 호스트)
./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest

# iOS Simulator 프레임워크 링크와 공통 테스트
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:iosSimulatorArm64Test

# 실제 iOS 기기용 프레임워크 링크
./gradlew :shared:linkDebugFrameworkIosArm64
```

앱 실행은 Android Studio의 Android 실행 구성 또는 Xcode의 `iosApp/iosApp.xcodeproj`를 사용합니다. Xcode 빌드 단계는 기존 `:shared:embedAndSignAppleFrameworkForXcode` 연결을 유지합니다.

새 기능 모듈은 `feature/<name>/build.gradle.kts`에 아래 플러그인을 적용하고, `settings.gradle.kts`와 `shared`의 의존성에 등록합니다.

```kotlin
plugins { id("woorisseum.compose.feature") }
```
