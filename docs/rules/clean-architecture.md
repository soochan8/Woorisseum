# 클린 아키텍처 규칙과 적용 방법

## 적용 범위

기능 추가, Repository·UseCase·모델 설계, 데이터 연동, 모듈 분리, DI와 내비게이션을 변경할 때 읽는다.

## 모듈 경계

화살표는 호출 순서가 아니라 코드·모듈의 의존 방향이다.

```text
androidApp / iosApp → shared
shared → feature:* / core:data / core:domain / core:designsystem
feature:* → core:domain / core:designsystem
core:data → core:domain
```

- domain은 비즈니스 모델, Repository 계약, 필요한 UseCase를 소유한다. Compose·Koin·Firebase·저장소 구현에 의존하지 않는다.
- data는 Repository 구현, 데이터 소스, DTO, Mapper를 소유한다. 외부 SDK 타입과 오류를 domain 계약에 노출하지 않는다.
- feature는 MVI 화면, ViewModel, UI 상태·Intent·필요한 Effect를 소유한다. data 구현이나 다른 feature를 직접 참조하지 않는다.
- designsystem은 테마와 재사용 UI만 소유한다. feature·domain·data에 의존하지 않고 비즈니스 계산을 하지 않는다.
- shared는 DI와 앱 내비게이션을 조립한다. feature는 이동 의도를 콜백으로 전달하고 다른 기능 화면을 직접 생성하지 않는다.
- domain/data는 `auth`, `household`, `transaction`, `budget`, `shopping` 등 비즈니스 기능별 패키지로 구분한다.

## 계약과 구현 연결

거래 목록을 추가한다면 다음처럼 책임을 배치한다. 아래 이름은 설계 예시이며 실제 구현이 존재한다는 뜻은 아니다.

```text
feature:transaction   TransactionViewModel → TransactionRepository 계약 사용
core:domain           Transaction, TransactionRepository
core:data             TransactionRepositoryImpl, TransactionDto, Mapper
shared                Repository 구현 binding과 feature 조립
```

- 생성자 주입을 기본으로 한다. ViewModel 내부에서 구현 객체나 DI 컨테이너를 직접 생성하지 않는다.
- Koin 정의는 구현을 소유한 모듈에 두고 shared에서 모아 초기화한다. domain 객체의 DI 정의는 조립부에 둔다.
- Repository 계약의 입출력은 도메인 타입을 사용한다. DTO 변환은 data의 Mapper에서 수행한다.
- 여러 동작의 조합, 비즈니스 규칙, 재사용할 동작이 있을 때만 UseCase를 추가한다. 단순 위임 UseCase를 일괄 생성하지 않는다.
- UI 표시 형식과 사용자 오류 문구는 feature에서 결정한다. 표시 전용 변환이 있을 때만 UI 모델을 추가한다.
- 화면 이동에는 ID 같은 최소 인자만 전달한다. Repository·ViewModel·전체 데이터 객체를 경로 인자로 전달하지 않는다.

## 적용 순서

1. 요청한 기능의 비즈니스 책임과 기존 모듈을 확인한다.
2. 필요한 domain 계약을 정한다. 기존 계약으로 충분하면 새 추상화를 만들지 않는다.
3. data 구현과 변환 경계를 연결한다.
4. feature의 MVI 흐름을 구성하고 shared에서 구현·화면 이동을 연결한다.
5. 의존 변경이 있으면 `./gradlew moduleRules`로 확인한다. 기존 위반은 작업과 구분해 보고한다.

## 피해야 할 구현

- feature에서 Firebase나 `RepositoryImpl`을 직접 사용하기.
- domain에서 `Context`, `UIViewController`, `NavController`, SDK 예외를 계약으로 노출하기.
- 화면 하나를 추가할 때마다 새 Gradle 모듈이나 api/impl 모듈을 만들기.
- 실제 구현이 없는 Firebase·DI 초기화·내비게이션을 이미 존재한다고 가정하기.

## 완료 전 확인

- 새 코드가 배치된 모듈의 책임에 맞는가?
- UI는 domain 계약에 의존하고, 구현 연결은 shared가 담당하는가?
- DTO·SDK 타입이 domain이나 feature 계약으로 새어 나오지 않는가?
