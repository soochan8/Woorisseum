# Woorisseum 아키텍처 설계 기준

## 문서 역할과 현재 상태

이 문서는 프로젝트의 모듈 책임, 모델 변환, 결과·상태 계약과 추상화 기준을 정의한다.
루트 [AGENTS.md](../../AGENTS.md)는 문서 진입점이며, 영역별 세부 적용 방법은 이 문서에서 연결하는 기존 규칙 문서를 따른다.

현재는 모듈·의존성·소스 디렉터리와 예제 화면이 준비된 단계다. 아래 내용은 앞으로 추가할 기능의 설계 기준이다.
Firebase 연동, 실제 Repository·ViewModel, 앱 내비게이션, DI 초기화와 `AppResult`가 이미 구현된 것으로 간주하지 않는다.
문서의 모듈 경계는 목표 설계이며, 실제 의존성·구현 상태는 소스와 Gradle 설정으로 확인한다.

## 모듈과 플랫폼 경계

Android·iOS는 Kotlin Multiplatform(KMP)로 로직을, Compose Multiplatform(CMP)로 UI를 공유한다.
공통 구현은 `commonMain`, 플랫폼 API 연결은 `androidMain`·`iosMain`에 둔다.

| 모듈 | 책임 |
| --- | --- |
| `androidApp` | Android 실행 진입점과 플랫폼 앱 설정 |
| `iosApp` | iOS 실행 진입점과 공통 Compose 호스트. Gradle 모듈이 아닌 Xcode 프로젝트 |
| `shared` | 공통 `App()`, iOS `MainViewController()`, 앱 전체 내비게이션과 DI 조립 |
| `core:domain` | 비즈니스 모델, Repository 인터페이스, 필요한 UseCase |
| `core:data` | Repository 구현, 데이터 소스, Entity·DTO, Mapper |
| `core:designsystem` | 공통 테마와 재사용 Compose 컴포넌트 |
| `feature:auth` | 로그인·회원가입 UI, ViewModel, UI 상태·Intent |
| `feature:household` | 공동 가계부 생성·참여 UI, ViewModel, UI 상태·Intent |
| `feature:transaction` | 거래 목록·등록·수정 UI, ViewModel, UI 상태·Intent |
| `feature:budget` | 월별 예산·통계 UI, ViewModel, UI 상태·Intent |
| `feature:shopping` | 공유 장보기 UI, ViewModel, UI 상태·Intent |

화살표는 실행 순서가 아닌 코드·모듈의 의존 방향이다.

```text
androidApp / iosApp → shared
shared → feature:* / core:data / core:domain / core:designsystem
feature:* → core:domain / core:designsystem
core:data → core:domain
```

Repository 계약은 domain, 구현은 data가 소유한다. feature는 data 구현이나 다른 feature를 직접 참조하지 않는다.
domain·data 내부는 비즈니스 기능별 패키지로 구분하며, 화면 개수에 맞춰 모듈을 추가하지 않는다.

의존성 역전, Koin binding의 배치, shared의 조립 책임과 화면 이동의 최소 인자는 [클린 아키텍처 규칙](clean-architecture.md)을 따른다.
플랫폼 인터페이스·`expect/actual`, 지원 타깃, 정적 `Shared` 프레임워크와 빌드 설정은 [KMP와 빌드 규칙](kmp-build.md)을 따른다.

## 모델 소유권과 Mapper

| 종류 | 위치 | 의미 |
| --- | --- | --- |
| Entity·DTO | data | DB 저장 형식 또는 외부 통신 형식 |
| 도메인 모델·입력 모델 | domain | 저장 방식과 화면 표시 형식에 독립적인 비즈니스 값·계약 |
| UI 모델 | feature | 금액·날짜 문구 등 표시용 변환이 필요한 데이터 |
| 화면별 UiState | feature | 입력·목록·로딩·저장 중·복구 가능한 오류 등 화면을 재구성하는 상태 |

- Entity·DTO는 data 내부에 두고 도메인 모델과 분리한다. Repository의 입출력에는 도메인 타입을 사용한다.
- 저장·통신 모델과 도메인 모델의 변환 함수는 data에, 도메인 모델과 UI 모델의 변환 함수는 feature에 둔다.
- 단순 변환은 `toDomain()`, `toEntity()`, `toUiModel()` 같은 순수 확장 함수를 기본으로 한다. 변환 책임에 외부 의존성이 실제로 필요한 경우에만 Mapper 클래스를 사용한다.
- Mapper는 값을 변환한다. DB 호출, 상태 변경, 업무 검증, 비즈니스 계산을 섞지 않는다.
- UI 모델은 표시용 변환이 필요할 때만 추가한다. 화면 상태에 도메인 모델을 직접 포함할 수 있다.
- 도메인의 확정된 금액과 입력 중인 문자열처럼 의미가 다른 값은 구분한다. 업무 규칙은 domain이 담당하고 입력·표시 형식은 feature가 담당한다.

지출 기능의 모델 경계 예시는 다음과 같다. 아래 타입은 구현이 존재한다는 뜻이 아니다.

```text
data: ExpenseEntity / ExpenseDto
          ↓ data의 toDomain()
domain: Expense
          ↓ 표시용 변환이 필요한 경우에만 feature의 toUiModel()
feature: ExpenseUiModel 또는 Expense를 포함한 ExpenseUiState
```

## 작업 결과와 오류 계약

- domain의 공통 `AppResult<T>`로 성공 값과 예상 가능한 실패를 표현한다. 성공은 `Success<T>`, 실패는 domain에서 정의한 `AppError`를 가진 `Failure`로 구분한다.
- `AppResult`에는 `Loading`, 화면 오류 문구, 플랫폼 객체를 넣지 않는다. 로딩은 작업 결과가 아닌 feature의 화면 상태다.
- data는 예상 가능한 저장소·SDK 실패를 도메인 오류로 변환한다. 외부 SDK 타입·예외를 domain 계약이나 UiState에 그대로 노출하지 않는다.
- 예상하지 못한 프로그래밍 오류까지 무조건 정상적인 실패 값으로 숨기지 않는다. `CancellationException`은 일반 실패로 변환하지 않고 전파한다.
- ViewModel은 작업 결과와 현재 화면 맥락에 따라 UiState를 갱신한다. 도메인 오류의 사용자 표시 문구는 feature에서 결정한다.
- 모델 Mapper로 `AppResult`를 공통 UiState에 기계적으로 변환하지 않는다. 저장 성공 시 입력 초기화·목록 반영·화면 이동 등 화면별 동작은 ViewModel이 결정한다.
- 각 레이어에 같은 Loading·Success·Error 래퍼를 반복해서 만들지 않는다. 데이터 모델, 작업 결과, 화면 상태를 서로 구분한다.

## 화면 상태와 MVI

- 공통 UI는 기존 MVI 계약을 유지한다. 화면별 `UiIntent`를 `onIntent(intent)`로 전달하고 ViewModel이 상태 전이를 소유한다.
- 화면별 불변 `data class UiState`를 기본으로 하며 입력·목록·로딩·저장 중·오류를 필요한 만큼 표현한다. 전체 화면을 항상 단일 Loading·Success·Error 중 하나로 제한하지 않는다.
- ViewModel은 읽기 전용 `StateFlow<UiState>`를 노출한다. 서로 모순되는 작업 상태가 생기지 않도록 구성하고, 상태가 복잡해지면 관련 상태를 묶어 표현한다.
- 도메인은 업무에 필요한 상태를 가질 수 있지만 UI의 로딩·저장 중 상태를 복제하지 않는다.

Intent·Effect 전달, reducer 도입 기준, 취소·경쟁 처리와 중요한 결과 보존은 [MVI 규칙](mvi.md)을 따른다.
화면 진입부·콘텐츠 분리, 상태 수집, 공통 UI·테마·리소스 배치는 [Compose 규칙](compose.md)을 따른다.

## 필요한 만큼의 추상화

- 현재 기능에 필요한 최소 구조로 시작하고 저장 방식·플랫폼·UI의 경계를 유지한다.
- UseCase는 업무 규칙, 여러 동작의 조합 또는 재사용할 동작이 있을 때 추가한다. Repository 호출을 한 번 감싸기 위한 계층을 일괄 생성하지 않는다.
- 구체적인 중복과 공통 책임이 확인되기 전에는 `BaseRepository`, `BaseViewModel`, `BaseMapper`를 만들지 않는다.
- 모든 도메인 모델에 대응하는 UI 모델·Mapper 클래스·인터페이스를 의무적으로 추가하지 않는다.

SOLID, KISS·YAGNI·DRY의 판단 기준은 [SOLID와 단순성 규칙](solid.md)을 따른다.
의존성의 모듈별 선언, version catalog, convention plugin과 `implementation`·`api` 구분은 [KMP와 빌드 규칙](kmp-build.md)을 따른다.
