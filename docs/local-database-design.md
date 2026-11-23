# 로컬 DB 초기 설계

## 이번 단계의 범위

이슈 #8의 1~2단계: 카드·지출 저장 형식과 Room 기반, 플랫폼 생성 설정, DI, 버전 1 스키마를 준비한다.
현재는 3단계의 카드 Repository·쓰기 DAO·Mapper와 카드 화면 오류 처리도 연결했다.
카드는 실제 로컬 DB에 저장하고, 지출 Repository·화면 저장 연결은 후속 단계다.
설계 기준은 [로컬 DB 규칙](rules/local-database.md)을 따른다.

## 저장 형식

| 테이블 | 값 | 저장 표현과 의미 |
| --- | --- | --- |
| cards | registrationOrder | 자동 증가 INTEGER 기본키. 기존 계약의 등록 순서 유지 |
| cards | id | 변경되지 않는 TEXT 식별자, UNIQUE. 후속 등록 구현에서 UUID 발급 |
| cards | alias, issuer | 필수 TEXT. 별칭 중복 허용 |
| expenses | id | TEXT 기본키. 후속 등록 구현에서 UUID 발급 |
| expenses | amount | INTEGER / Kotlin Long, 원화 정수. 업무 규칙은 1원 이상 |
| expenses | dateEpochDay | INTEGER / Long, 1970-01-01 기준 일수. 시간대 변환 없음 |
| expenses | merchant, memo | 선택 TEXT, 미입력은 null |
| expenses | category | 안정적인 저장 코드. FIXED / VARIABLE / IMPULSE로 매핑 예정 |
| expenses | paymentType | CASH / BANK_TRANSFER / CARD로 매핑 예정 |
| expenses | cardId, cardName, cardIssuer | 카드 결제 당시 정보. 다른 결제수단이면 모두 null |
| expenses | revision | 로컬 전체 수정 충돌 검사에 사용할 INTEGER 버전, 초기값 0 |

저장 코드는 enum ordinal이나 표시 문자열을 사용하지 않는다. 후속 Mapper에서 명시적으로 변환한다.
서버 시각·삭제 시각·동기화 플래그는 아직 추가하지 않는다. 로컬 revision은 서버 버전이 아니다.

## 조회·참조·제약

- 카드 목록은 registrationOrder 오름차순이다. id에는 고유 인덱스를 둔다.
- 지출은 dateEpochDay·id 복합 인덱스로 기간을 조회하고 날짜 내림차순, 같은 날짜는 id 내림차순으로 안정적으로 정렬한다. 같은 날짜의 등록 순서를 의미하지 않는다.
- 기간은 시작일 포함·종료일 제외다. 월별 조회는 해당 월 1일부터 다음 달 1일 전까지로 계산한다. 사용자 종료일 포함 입력은 후속 조회 구현에서 이 경계로 변환한다.
- 지출의 cardId는 과거 기록용 식별자다. cards에 외래키를 걸지 않아 등록 카드 삭제 후에도 당시 별칭·카드사를 보존한다.
- 카드 삭제·이름 변경으로 기존 지출의 스냅샷을 갱신하지 않는다.
- 이번 스키마가 보장하는 제약은 기본키·NOT NULL·카드 ID 고유성이다. 금액·문자열·결제수단별 필드 조합 검증은 후속 쓰기 구현에서 추가한다. 카드 쓰기는 필수값·공백을 Repository에서 검증한다. 지출 쓰기 DAO는 아직 제공하지 않는다.
- 로컬 단계는 단일 가계부 범위다. 계정·공동 가계부 연결 전 householdId 추가와 기존 데이터 귀속을 별도 설계·마이그레이션한다. 기기 내 저장을 실제 공유로 취급하지 않는다.

## 동시성과 트랜잭션 경계

- 앱 진입점에서 시작한 Koin이 단일 DB 인스턴스를 보유한다. 화면 재생성 때 DI·DB를 다시 시작하지 않는다. Preview는 DB 없는 별도 DI를 사용한다.
- 비동기 DAO와 DB의 IO 코루틴 컨텍스트를 사용하며 메인 스레드 동기 쿼리는 허용하지 않는다.
- 후속 카드 등록은 고유 ID로 삽입하고 기본 충돌 정책으로 실패시킨다. REPLACE로 기존 행을 삭제·재생성하지 않는다.
- 후속 지출 전체 수정은 ID와 revision으로 조건부 갱신하고 영향받은 행 수로 충돌·항목 없음을 처리한다. 저장 성공 시 revision을 증가시킨다.
- 하나의 INSERT·조건부 UPDATE·DELETE는 해당 SQL의 원자성을 활용한다. 여러 SQL의 일관성이 필요할 때 명시적 트랜잭션을 추가한다.
- 카드 삭제와 지출 정보 삭제를 묶지 않는다. 과거 스냅샷을 보존한다.
- 카테고리 삭제·순서 변경·장보기 완료의 복합 트랜잭션은 장보기 저장 연결 때 구현한다.

## 플랫폼·스키마 수명

- Android: Application Context의 getDatabasePath를 사용한다.
- iOS: Application Support 디렉터리를 생성해 저장한다. 캐시·임시 디렉터리를 사용하지 않는다.
- 파일명은 두 플랫폼 모두 woorisseum.db다. 파일 자체는 각 기기에 독립적으로 존재한다.
- Room 스키마 버전은 1이며 core/data/schemas에 내보낸 JSON을 버전 관리한다.
- 이전 배포 DB가 없어 초기 마이그레이션은 없다. 후속 스키마 변경은 데이터 보존 마이그레이션을 추가하며 파괴적 fallback은 설정하지 않는다.
- DB 생성은 DI에서 지연 실행되며 카드 Repository가 최초로 필요할 때 생성한다. 실제 파일 생성·재실행 후 카드 저장 유지는 카드 저장 연결 단계에서 확인한다.

## 카드 저장 연결

- 운영 CardRepository는 LocalCardRepository에 바인딩하고 Preview만 별도 메모리 구현을 사용한다.
- UUID 식별자와 자동 증가 등록 순서를 분리한다. 수정은 단일 UPDATE로 등록 순서·ID를 보존하며, 대상 행이 없으면 실패한다.
- 카드 삭제는 cards 테이블에만 적용한다. 이미 없는 ID 삭제도 성공한다.
- 카드 관찰은 AppResult를 전달한다. 예상 가능한 SQLite 오류는 Failure 후 종료하며 화면의 재시도는 새 구독을 만든다. 취소·프로그래밍 오류는 숨기지 않는다.
- 카드 관리의 로딩·조회 실패·빈 목록을 구분한다. 지출의 카드 선택·필터도 조회 실패를 표시하고 재시도할 수 있다.
- 카드 편집은 현재 단일 사용자 로컬 작업 기준이다. 다른 기기·사용자의 동시 수정 충돌 감지는 서버 연결 단계의 별도 정책이며 이번 구현으로 보장하지 않는다.
