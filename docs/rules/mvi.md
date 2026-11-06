# MVI 상태·이벤트 처리 규칙

## 적용 범위

Android와 iOS가 공유하는 화면의 ViewModel, 상태, 사용자 이벤트, 비동기 처리와 UI 효과를 변경할 때 읽는다.
MVI를 위해 별도 프레임워크를 도입하지 않고 기존 ViewModel·Coroutines·StateFlow로 구현한다.

## 기본 흐름과 계약

```text
Compose UI → UiIntent → ViewModel → domain 동작
                            ↓
                         UiState → Compose UI
                            └─ 필요한 일회성 요청 → UiEffect → 화면 진입부의 콜백
```

- 화면별로 `<Feature>UiState`, `<Feature>UiIntent`, `<Feature>ViewModel`을 정의한다.
- Intent는 `sealed interface` 등으로 표현하고 ViewModel의 `onIntent(intent)`에서 처리한다. Android의 `Intent` 타입과 구분한다.
- UiState는 불변 값으로 표현한다. 변경 가능한 컬렉션이나 `MutableStateFlow`를 외부로 노출하지 않는다.
- ViewModel은 내부 `MutableStateFlow`와 외부 읽기 전용 `StateFlow`를 구분하고 상태 변경을 소유한다.
- 로딩·데이터·입력·복구 가능한 오류 등 화면을 다시 그릴 때 필요한 값은 State로 표현한다. 서로 모순되는 상태를 만들지 않는다.
- 복잡한 전이를 정리할 필요가 있을 때 순수 reducer 함수를 추가할 수 있다. 모든 화면에 Reducer·Store·Middleware 계층을 강제하지 않는다.

## 기능 예시

거래 화면의 계약은 다음과 같은 의미로 정의한다. 실제 필드와 이벤트는 요구사항에 맞춰 최소한으로 정한다.

| 사용자·화면 동작 | Intent 예시 | 처리 결과 |
| --- | --- | --- |
| 목록 새로고침 | `Refresh` | 로딩 상태와 새 목록 또는 오류 |
| 금액 입력 | `AmountChanged(value)` | 입력 상태 갱신 |
| 저장 요청 | `Submit` | 중복 제출 방지, 저장 결과 반영 |
| 거래 선택 | `TransactionClicked(id)` | 필요한 경우 상세 이동 Effect |

화면 콘텐츠는 `state`와 `onIntent`를 받는다. 재사용 컴포넌트는 `onClick`·`onValueChange` 같은 콜백을 받고 feature가 Intent로 변환한다.

## State와 Effect 구분

- 재진입·재수집 후에도 복구되어야 하는 중요한 결과는 State 또는 영속 데이터로 관리한다.
- 화면 이동·스낵바 등 일회성 UI 요청이 필요할 때만 `<Feature>UiEffect`와 읽기 전용 이벤트 스트림을 추가한다.
- Effect에 `NavController`·플랫폼 객체를 넣지 않는다. 이동 대상의 최소 ID 등을 전달하고 화면 진입부가 콜백으로 변환한다.
- Flow·Channel이라는 이유만으로 정확히 한 번 전달되거나 재진입 시 복원된다고 가정하지 않는다. 수집 중단 시 유실·재전달 가능성과 버퍼 동작을 요구사항에 맞춰 정한다.
- 저장 성공처럼 중요한 사실을 유실될 수 있는 Effect 하나로만 표현하지 않는다.
- Focus·단순 펼침 같은 일시적인 표현 상태는 로컬 Compose 상태로 관리할 수 있다. 도메인 작업과 연관된 상태는 ViewModel이 소유한다.

## 비동기와 상태 전이

- ViewModel 생명주기에 맞는 scope에서 실행한다. `GlobalScope`를 사용하지 않는다.
- `MutableStateFlow.update`와 reducer는 부수효과 없이 상태만 계산한다. 내부에서 I/O·코루틴 실행·Effect 발행을 하지 않는다.
- 중복 저장·새로고침·검색 경쟁은 기능에 맞게 처리한다. 제출 중 추가 제출 방지, 검색의 이전 요청 취소 등 정책을 명확히 한다.
- 오래된 요청의 완료가 최신 입력·조회 결과를 덮어쓰지 않도록 한다. 모든 Intent를 무조건 직렬화하거나 모두 병렬 실행하지 않는다.
- `CancellationException`은 일반 실패로 변환하지 않는다. 광범위한 catch 또는 `runCatching`을 사용한다면 취소 전파를 보장한다.
- 외부 SDK 예외를 UiState에 그대로 넣지 않는다. domain 계약에 맞는 실패를 UI 오류 표현으로 변환한다.

## 완료 전 확인

- 비즈니스 관련 사용자 동작이 Intent를 통해 들어오는가?
- 상태 변경 책임이 ViewModel에 모여 있고 화면이 직접 데이터를 변경하지 않는가?
- 로딩 종료·오류·중복 요청·취소 시에도 상태가 일관되는가?
- Effect가 유실되어도 중요한 데이터와 결과가 보존되는가?
