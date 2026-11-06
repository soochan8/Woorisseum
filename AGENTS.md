# Woorisseum 아키텍처와 설계 원칙

## Codex 작업 시 규칙 문서 읽기

작업을 시작할 때 아래 표에서 해당 작업에 적용되는 문서를 먼저 읽고 따른다. 여러 영역을 변경하면 관련 문서를 모두 읽는다.
문서 경로는 저장소 루트 기준이다. 링크만으로 상세 문서가 자동 로드된다고 가정하지 말고 필요한 문서를 직접 연다.

| 규칙 문서 | 읽어야 하는 작업 |
| --- | --- |
| [아키텍처 설계 기준](docs/rules/architecture.md) | 설계, 모듈·계층·모델·Mapper·상태·오류 처리·의존성 변경 |
| [클린 아키텍처](docs/rules/clean-architecture.md) | 기능 추가, domain/data/feature 변경, Repository·UseCase·DI·내비게이션 작업 |
| [MVI](docs/rules/mvi.md) | 화면 상태, 사용자 이벤트, ViewModel, 비동기 처리, UI 효과 작업 |
| [Compose](docs/rules/compose.md) | Compose 화면·컴포넌트·테마·리소스·접근성 작업 |
| [SOLID와 단순성](docs/rules/solid.md) | Kotlin 운영 코드 설계·구현·리팩토링 전반 |
| [KMP와 빌드](docs/rules/kmp-build.md) | 소스셋, 플랫폼 API, Gradle, 라이브러리, 모듈 구조·Baseline 작업 |

- 이 파일은 공통 원칙과 진입점이며 상세 적용 방법·예시는 연결된 문서에 둔다. 규칙을 변경하면 서로 모순되지 않도록 함께 갱신한다.
- 문서는 설계 기준이다. 실제 구현 여부·라이브러리 API·의존성은 소스와 Gradle 설정으로 확인한다.
- 현재 작업 범위 밖의 사용자 변경과 테스트용 의존성을 임의로 수정하지 않는다. 검사 실패를 숨기기 위해 Baseline을 갱신하지 않는다.
- 사용자가 별도로 요청하기 전에는 테스트 코드를 추가·수정하지 않는다. 기존 테스트는 삭제하지 않는다.
- README는 사용자가 변경을 요청한 경우에만 수정한다.

## 프로젝트 구성

Android·iOS에서 Kotlin Multiplatform(KMP)로 비즈니스 로직을, Compose Multiplatform(CMP)로 UI를 공유한다.
공통 구현은 `commonMain`에 두고, 플랫폼 API 연결은 `androidMain`·`iosMain`에 배치한다.
플랫폼별 차이가 있는 경계에 인터페이스 또는 필요한 `expect`/`actual`을 사용한다.

현재는 모듈·의존성·소스 디렉터리와 예제 화면이 준비된 단계다. 아래 원칙은 앞으로 추가할 기능의 설계 기준이다.
Firebase 연동, 실제 Repository·ViewModel, 앱 내비게이션과 DI 초기화가 이미 구현된 것으로 간주하지 않는다.

| 모듈 | 책임 |
| --- | --- |
| `androidApp` | Android 실행 진입점과 플랫폼 앱 설정 |
| `iosApp` | iOS 실행 진입점과 공통 Compose 화면을 표시하는 호스트. Gradle 모듈이 아닌 Xcode 프로젝트 |
| `shared` | 공통 `App()`, iOS `MainViewController()`, 앱 전체 내비게이션과 DI 조립 |
| `core:domain` | 비즈니스 모델, Repository 인터페이스, 필요한 UseCase |
| `core:data` | Repository 구현, 데이터 소스, DTO, Mapper |
| `core:designsystem` | 공통 테마와 재사용 Compose 컴포넌트 |
| `feature:auth` | 로그인·회원가입 UI, ViewModel, UI 상태 |
| `feature:household` | 공동 가계부 생성·참여 UI, ViewModel, UI 상태 |
| `feature:transaction` | 거래 목록·등록·수정 UI, ViewModel, UI 상태 |
| `feature:budget` | 월별 예산·통계 UI, ViewModel, UI 상태 |
| `feature:shopping` | 공유 장보기 UI, ViewModel, UI 상태 |

## 클린 아키텍처와 의존 방향

화살표는 코드·모듈의 의존 방향을 나타낸다.

```text
androidApp / iosApp → shared
shared → feature:* / core:data / core:domain / core:designsystem
feature:* → core:domain / core:designsystem
core:data → core:domain
```

- `core:domain`은 UI, 저장소 구현, DI 프레임워크에 의존하지 않는다. 공통 비동기·날짜 라이브러리는 사용할 수 있다.
- Repository 인터페이스는 domain에 두고 data에서 구현한다. UI 계층은 domain 계약을 사용하고, 실제 구현 연결은 shared에서 조립한다.
- feature는 data 구현이나 다른 feature를 직접 참조하지 않는다. 모듈 사이에 순환 의존성을 만들지 않는다.
- designsystem은 feature, domain, data에 의존하지 않는다.
- domain·data 내부는 `auth`, `household`, `transaction`, `budget`, `shopping` 등 기능별 패키지로 구분한다.
- 모듈은 비즈니스 책임과 의존 경계를 기준으로 나눈다. 화면 개수에 맞춰 모듈을 추가하지 않는다.
- UseCase는 비즈니스 규칙, 여러 동작의 조합, 재사용할 동작이 있을 때 추가한다. 단순 위임만 하는 계층을 일괄 생성하지 않는다.

## UI·상태·데이터 흐름

- Android·iOS의 공통 Compose UI는 MVI와 단방향 데이터 흐름을 기본으로 한다. ViewModel은 Intent 처리와 상태 보관을 담당한다.
- 화면별 `UiState`, `UiIntent`를 정의하고 `onIntent(intent)`로 이벤트를 전달한다. 일회성 UI 요청이 필요할 때만 `UiEffect`를 추가한다.
- ViewModel은 읽기 전용 `StateFlow<UiState>`를 노출하고, 변경 가능한 상태를 내부에 보관한다.
- 화면 진입 컴포저블은 ViewModel과 생명주기를 고려한 상태 수집을 연결한다. 화면 콘텐츠는 상태와 사용자 동작 콜백을 받는다.
- Compose 화면은 데이터 소스 호출이나 비즈니스 계산을 직접 수행하지 않는다. 일시적인 포커스·펼침 상태 등 화면 내부 동작은 로컬 UI 상태로 관리할 수 있다.
- 사용자 동작은 ViewModel에서 처리하고 결과는 UI 상태로 반영한다. 내비게이션 등 UI 효과를 도메인 모델에 포함하지 않는다.
- DTO는 data 내부에 두고 Mapper를 통해 도메인 모델로 변환한다. 표시 전용 변환이 필요한 경우에만 feature에 UI 모델을 추가한다.
- 외부 SDK의 타입·오류를 도메인 계약에 노출하지 않는다. data에서 구현 세부사항을 숨기고, UI에 표시할 오류 표현은 UI 계층에서 결정한다.
- 비동기 취소는 일반 오류로 변환하지 않는다. `CancellationException`을 처리 경계 밖으로 전파한다.

## 의존성 주입과 내비게이션

- Koin 정의는 해당 구현을 소유한 모듈에 두고 shared에서 조립·초기화한다. 생성자 주입을 기본으로 한다.
- domain은 Koin을 참조하지 않는다. domain 객체의 DI 정의는 조립부에서 제공한다.
- 앱 전체 내비게이션은 shared가 소유한다. feature는 이동 의도를 콜백으로 전달하고 다른 기능의 화면을 직접 생성하지 않는다.
- 화면 이동 시 식별자 등 필요한 최소한의 인자를 전달한다. Repository, ViewModel, 전체 데이터 객체를 화면 인자로 전달하지 않는다.

## 공통 UI와 리소스

- 여러 기능에서 사용하는 테마·컴포넌트는 designsystem에, 기능 전용 UI는 해당 feature에 둔다.
- 공통 컴포넌트는 화면별 비즈니스 로직을 포함하지 않고 상태·콜백을 전달받는다.
- 앱에 표시하는 문자열·이미지는 Compose Resources를 사용한다. 기능 전용 리소스는 해당 기능에, 여러 기능에서 공유하는 UI 리소스는 designsystem에 둔다.

## 빌드와 의존성 구성

- 라이브러리와 프로젝트 모듈 의존성은 각 모듈의 `build.gradle.kts`에 명시한다.
- 라이브러리 버전·좌표는 `gradle/libs.versions.toml`에서 관리한다.
- KMP 타깃·컴파일러·Compose 플러그인 등 반복 빌드 설정은 `build-logic`의 convention plugin에서 관리한다.
- 기본 의존성 선언은 `implementation`이다. 공개 API의 타입 노출에 필요한 경우에만 `api`를 사용한다.
- 공통 모듈은 Android, iOS arm64, iOS Simulator arm64를 지원한다. Android 전용 라이브러리는 `commonMain`에 배치하지 않는다.
- iOS용 정적 `Shared` 프레임워크는 shared에서 생성한다. 내부 모듈은 필요한 경우에만 Swift API로 명시적으로 노출한다.
- 모듈 구조와 소스 배치에 대한 설명은 `README.md`, 실제 의존성 구성은 Gradle 설정을 함께 참고한다.

## 핵심 아키텍처 원칙

- KMP·CMP로 Android·iOS의 로직과 UI를 공유하고 클린 아키텍처와 MVI를 사용한다.
- feature는 domain 계약을 사용하고, data가 구현하며, shared가 DI와 앱 내비게이션을 조립한다.
- 현재 기능에 필요한 최소 구조를 유지한다. 불필요한 Base 클래스·UseCase·UI 모델을 일괄 생성하지 않는다.
- 설계·모델·상태·오류 처리·의존성을 변경할 때는 [아키텍처 설계 기준](docs/rules/architecture.md)을 직접 읽고 따른다.
