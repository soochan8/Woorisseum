# Compose Multiplatform UI 규칙

## 적용 범위

공통 화면, 재사용 컴포넌트, 테마, 리소스와 UI 생명주기를 변경할 때 읽는다.
상태·이벤트 계약을 변경한다면 [MVI 규칙](mvi.md)도 함께 적용한다.

## 화면 진입부와 콘텐츠 분리

- `<Feature>Route` 같은 화면 진입부에서 ViewModel 연결, 생명주기를 고려한 State 수집, Effect 수집과 이동 콜백을 담당한다.
- `<Feature>Screen` 같은 콘텐츠는 State와 사용자 이벤트 콜백을 받는다. DI·Repository·ViewModel에 직접 접근하지 않는다.
- 상태 수집은 공통 Lifecycle API인 `collectAsStateWithLifecycle()`을 기본으로 한다. Android 전용 API를 공통 코드에 가져오지 않는다.
- Effect 수집은 `LaunchedEffect` 등 Compose 생명주기에 맞춰 연결하고 동일 Effect에 중복 수집자를 만들지 않는다.
- 최초 로딩은 ViewModel 초기화 또는 안정적인 화면 인자를 key로 한 진입 이벤트 중 한 곳에서만 시작한다. 둘 다 호출해 중복 조회하지 않는다.

예시 구조는 다음과 같다. 구현 시 feature 계약과 실제 의존성에 맞게 작성한다.

```text
TransactionRoute
  ├─ ViewModel 연결 및 상태 수집
  ├─ Effect → shared가 전달한 내비게이션 콜백
  └─ TransactionScreen(state, onIntent)
       └─ 공통 컴포넌트(value, onValueChange, modifier)
```

## 재구성과 로컬 상태

- Composable 본문에서 네트워크·DB 호출, 직접 코루틴 실행, 외부 상태 변경을 하지 않는다.
- UI 재구성으로 중복 작업이 발생하지 않도록 `LaunchedEffect`·`DisposableEffect`의 key와 정리 책임을 정한다.
- 일반 지역 변수로 상태를 유지하지 않는다. 일시적인 UI 상태에는 `remember`, 복원이 필요한 저장 가능한 UI 값에는 `rememberSaveable` 등을 사용한다.
- State에 이미 있는 값을 별도 로컬 State로 복제하지 않는다. 저장이 필요한 입력값은 MVI 상태의 소유자를 먼저 정한다.
- 무거운 계산과 비즈니스 계산을 렌더링 과정에 넣지 않는다. `remember`·`derivedStateOf`·안정성 어노테이션을 근거 없이 일괄 추가하지 않는다.
- `LazyColumn` 등 변경 가능한 목록에는 안정적이고 고유한 item key를 사용한다.

## 컴포넌트 설계와 리소스

- 재사용 가능한 시각 컴포넌트는 기본값이 있는 `modifier: Modifier = Modifier`를 받고 루트 UI에 적용한다.
- 공통 컴포넌트는 값과 콜백을 받는다. 특정 feature의 Intent·ViewModel·도메인 규칙에 의존하지 않는다.
- 여러 기능이 공유하는 컴포넌트·테마는 designsystem에, 기능 전용 UI는 해당 feature에 둔다.
- 기존 테마와 컴포넌트를 먼저 사용한다. 화면마다 색·글자 스타일을 반복 정의하지 않고 공통 시각 값은 designsystem에서 관리한다.
- 사용자에게 표시하는 문자열·이미지는 Compose Resources를 사용한다. 공유 UI 리소스는 designsystem, 기능 전용 리소스는 feature에 둔다.
- 플랫폼별 콘텐츠 영역·키보드·스크롤을 고려한다. 기기 크기를 고정값으로 가정하지 않는다.
- 아이콘 버튼 등 의미가 필요한 요소는 리소스 기반 접근성 설명을 제공한다. 순수 장식 이미지는 불필요한 설명을 넣지 않는다.
- Preview는 DI·실제 저장소 없이 예시 State로 렌더링할 수 있게 구성한다. Preview를 위해 운영 코드에 가짜 데이터를 섞지 않는다.

## 완료 전 확인

- 콘텐츠를 State와 콜백만으로 렌더링할 수 있는가?
- 재구성·화면 재진입으로 조회와 Effect 처리가 중복되지 않는가?
- 공통 코드에 플랫폼 전용 API가 섞이지 않았는가?
- 문자열·테마·접근성이 기존 UI 규칙을 따르는가?
