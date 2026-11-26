# 함께 쓰기 API 계약 v1

## 상태와 범위

2026-11-26 기준 이슈 #9의 서버 설계 3단계다. [테이블·초대 정책](household-invitation-design.md)을 바탕으로 앱과 서버가 주고받을 계약을 정의한다. [2차 SQL](../supabase/migrations/20261126000200_household_profile_state.sql)의 프로필·상태 조회 함수는 사용자 적용 확인까지 완료했다. [3차 SQL](../supabase/migrations/20261126000300_household_invitations.sql)의 초대 함수는 로컬 PostgreSQL 검증을 완료했으며, 원격 적용·앱 DTO 연결은 아직 진행하지 않았다.

- 이 문서는 계약이다. 사용자 실행 결과로 [1차 SQL](../supabase/migrations/20261126000100_household_schema.sql)의 테이블·RLS 적용을 확인했다. 2차 SQL도 사용자 적용 확인을 받았다. 3차 SQL도 사용자 적용 확인을 받았다. 프로필·상태 조회 앱 연결을 추가했으며 Room DB는 변경하지 않았다.
- API는 Supabase PostgreSQL 함수 RPC로 제공한다. 각 함수는 단일 JSON 객체를 반환한다.
- 프로필 저장과 초대 발급은 별도 작업이다. 프로필 저장 성공 후 초대가 실패해도 프로필을 취소하지 않는다. 초대 발급·수락 내부의 관계 변경은 각각 원자적으로 처리한다.
- 프로필·상태 조회 SQL과 실행 권한은 2차 파일에 구현했다. 3차 SQL은 초대 처리 SQL을 구현했고 로컬 PostgreSQL 17.12의 다중 연결로 경합을 검증했다. Supabase의 실제 JWT·API 경유 검증은 이후 앱 연결 단계다. 이 문서가 검증을 대신하지 않는다.

## 공통 요청 규칙

모든 함수는 `POST /rest/v1/rpc/{함수명}`으로 호출한다.

- `apikey`: 프로젝트 Publishable key.
- `Authorization`: 해당 사용자의 access token을 담은 Bearer 인증. Publishable key만으로는 호출할 수 없다. 익명 인증 사용자도 유효한 세션으로 호출한다.
- JSON 본문의 `p_` 이름은 PostgreSQL 함수 인자명과 정확히 일치시킨다.
- 호출자의 사용자 ID는 검증된 세션의 `auth.uid()`로 결정한다. 앱에서 사용자 ID·권한·정원·만료 시각을 지정하는 인자를 받지 않는다.
- UUID는 문자열, 시각은 UTC RFC 3339 문자열, revision은 양의 64비트 정수로 표현한다. 앱은 날짜 정밀도를 임의로 줄이지 않는다.
- 토큰·인증 헤더·원본 링크를 로그·분석 이벤트·오류 문구에 넣지 않는다.

서버가 정상 실행되면 HTTP 200과 아래 업무 결과를 반환한다. **HTTP 200만 보고 성공으로 판단하지 않는다.**

성공:

```json
{"ok": true, "data": {}, "error": null}
```

예상 가능한 업무 실패:

```json
{"ok": false, "data": null, "error": {"code": "PROFILE_REQUIRED", "field": null}}
```

- `ok=true`면 `data`는 함수별 성공 객체이고 `error=null`이다.
- `ok=false`면 `data=null`이며 `error.code`는 아래의 고정 코드다. `field`는 입력 항목을 특정할 때만 사용한다.
- 서버는 한국어 화면 문구나 SDK 예외를 업무 응답에 넣지 않는다. 앱에서 코드에 맞는 리소스를 표시한다.
- 인증 게이트웨이·실행 권한·DB·통신 문제는 이 envelope 없이 HTTP 401/403/429/5xx 또는 응답 없음으로 끝날 수 있다. 이를 별도로 처리한다.
- 변경 후 업무 실패를 반환할 때에는 해당 작업의 변경을 먼저 롤백해야 한다. 실패 envelope를 반환하는 것만으로 롤백됐다고 가정하지 않는다. 예상하지 못한 예외는 변경 전체를 중단하고 원본 SQL 오류를 사용자에게 노출하지 않는다.

## 표시 프로필 계약

프로필 객체:

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| `user_id` | UUID 문자열 | 사용자 식별자. |
| `display_name` | 문자열 | 서버가 공백을 정리한 표시 이름. |
| `emoji_code` | 문자열 | 기존 8종 중 하나인 안정적인 저장 코드. |
| `revision` | 양의 정수 | 생성 시 1, 실제 수정마다 1 증가하는 서버 버전. |

이름은 앞뒤 공백 제거 후 **1~30 Unicode 코드포인트**로 제한한다. 사용자 확인을 거친 정책이며 UTF-16 `String.length`와 구분한다. 동일 이름·이모지는 허용한다.

공백 제거는 양끝의 Unicode White_Space 문자만 제거한다. 해당 집합은 U+0009~000D, U+0020, U+0085, U+00A0, U+1680, U+2000~200A, U+2028, U+2029, U+202F, U+205F, U+3000이다. 중간 공백을 합치거나 대소문자·Unicode 정규화를 변경하지 않는다. 앱·서버 구현 시 같은 규칙을 사용한다.

이모지 코드는 `CAT`, `DOG`, `RABBIT`, `BEAR`, `FOX`, `PANDA`, `FROG`, `PENGUIN`이다. 알 수 없는 코드는 입력 오류로 거절한다.

### save_my_profile_v1 — 생성·수정

요청:

```json
{"p_display_name": "수찬", "p_emoji_code": "CAT", "p_expected_revision": null}
```

- 최초 생성은 `p_expected_revision=null`로 요청한다. 기존 프로필 수정은 마지막으로 조회한 revision을 전달한다.
- 서버가 자신의 프로필 행을 잠그고 입력·버전을 검사한다.
- 입력이 현재 저장값과 같으면 revision을 올리지 않고 현재 프로필을 반환한다. 응답 유실 후 같은 입력의 재시도도 같다.
- 저장값과 다르면 기존 revision이 요청값과 일치할 때만 수정한다. 프로필이 없는데 기존 revision을 보내거나, 다른 값이 저장된 프로필에 null을 보내면 `PROFILE_CONFLICT`다.
- 같은 프로필의 동시 최초 생성은 사용자 PK와 서버 잠금으로 조정한다. 사용자에게 원본 unique 오류를 반환하지 않는다.

성공 `data` 예:

```json
{"profile": {"user_id": "00000000-0000-4000-8000-000000000001", "display_name": "수찬", "emoji_code": "CAT", "revision": 1}}
```

업무 오류: `INVALID_ARGUMENT`, `PROFILE_CONFLICT`.

프로필 저장만으로 공간·초대를 생성하거나 연결 완료를 표시하지 않는다.

## 현재 상태 계약

### get_together_state_v1 — 재실행·결과 확인

요청은 빈 객체 `{}`다. 자신의 프로필·현재 공간·구성원·본인 공간의 초대 메타데이터를 **일관된 스냅샷**으로 조회한다. 여러 앱 요청의 결과를 합쳐 같은 시점의 상태라고 가정하지 않는다.

성공 `data`:

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| `profile` | 프로필 객체 또는 null | 없으면 이름·이모지 설정이 필요하다. |
| `connection_state` | `NOT_CONNECTED` / `INVITING` / `CONNECTED` | 현재 서버 관계에서 계산한다. |
| `household` | 공간 객체 또는 null | 자신의 현재 열린 공간. |
| `invitation` | 초대 메타데이터 또는 null | 자신의 1인 공간에 남은 미처리 초대 하나. 원본 토큰·해시는 제외한다. |

공간 객체는 `household_id`, `members`를 가진다. 각 member는 위 프로필 객체와 같다. 배열 순서는 사용자 UUID 오름차순이며 앱은 `user_id`로 본인과 상대를 구분한다. slot이나 발급자 순서로 본인을 추측하지 않는다.

초대 메타데이터는 `invitation_id`, `status`(`AVAILABLE` / `EXPIRED`), `expires_at`이다. 수락·무효화된 과거 초대 목록은 반환하지 않는다.

일관성:

- `NOT_CONNECTED`: 공간·초대는 null. 프로필은 존재할 수도, 없을 수도 있다.
- `INVITING`: 프로필 존재, 열린 공간에 본인만 있다. 초대가 만료돼도 이 관계는 유지되며 초대 status로 만료를 구분한다.
- `CONNECTED`: 열린 공간에 본인을 포함한 서로 다른 구성원 두 명이 있고 초대 메타데이터는 null이다.
- 프로필이 없으면 정상 성공과 `profile=null`로 반환한다. 인증 오류·조회 실패를 이 상태로 숨기지 않는다.
- 닫힌 공간이나 모순된 구성원 관계는 연결된 것으로 반환하지 않는다. 데이터 무결성 오류로 처리한다.

예 — 아직 프로필을 저장하지 않은 사용자:

```json
{"profile": null, "connection_state": "NOT_CONNECTED", "household": null, "invitation": null}
```

## 초대 발급 계약

### create_household_invitation_v1 — 최초 발급·명시적 재발급

요청:

```json
{"p_request_id": "00000000-0000-4000-8000-000000000101"}
```

- 앱은 사용자 발급 동작마다 UUID를 한 번 만든다. 같은 동작의 중복 전송에는 같은 ID를 사용한다.
- 자신의 프로필이 필요하며, 이미 두 사람이 연결된 사용자는 새 초대를 발급할 수 없다.
- 자신의 공간이 없으면 공간·본인 구성원·초대를 함께 생성한다. 1인 준비 공간이 있으면 재사용한다.
- 새 요청 ID의 발급은 이전 미사용 초대를 무효화한다. 새 초대는 서버 발급 시각부터 24시간 유효하다.
- 최초 성공 응답의 `token`은 32바이트 난수의 padding 없는 base64url 표현이다. 길이는 43자다.

성공 `data` 예 — 아래 토큰은 형식 설명용이며 실제 초대가 아니다:

```json
{
  "invitation_id": "00000000-0000-4000-8000-000000000201",
  "household_id": "00000000-0000-4000-8000-000000000301",
  "token": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
  "expires_at": "2026-11-27T03:00:00Z"
}
```

동일 요청 ID 처리:

- 초대에 `create_request_id`를 저장하고 `(created_by, create_request_id)`를 UNIQUE로 제한한다.
- 같은 ID가 이미 커밋됐으면 다시 발급·무효화하지 않고 `ISSUANCE_RESULT_UNAVAILABLE`을 반환한다. 발급 결과를 조회해도 원본 토큰은 복원할 수 없다는 의미다.
- 이 보장은 **중복 부수효과 방지**다. 원본 토큰을 보관하지 않으므로 같은 성공 응답의 재생을 보장하지 않는다.
- 이전 작업이 롤백됐다면 행이 없으므로 같은 ID로 다시 실행할 수 있다. DB 잠금·고유 제약 아래에서 검사한다.
- 성공 응답을 잃으면 현재 관계를 재조회한다. 링크가 없을 때 사용자에게 재발급을 제안하고, 동의한 재발급 동작에만 새 요청 ID를 만든다. 통신 실패 직후 자동으로 새 ID를 만들지 않는다.
- 앱이 기존 발급 성공 응답과 중복 요청 오류를 모두 받은 경우, 같은 동작의 오류로 이미 받은 정상 링크를 덮어쓰지 않는다.

업무 오류: `INVALID_ARGUMENT`, `PROFILE_REQUIRED`, `ALREADY_CONNECTED`, `ISSUANCE_RESULT_UNAVAILABLE`.

서버는 토큰을 반환하고 실제 초대 URL 조립·플랫폼 공유는 앱의 shared 경계에서 처리한다. 링크 도메인·경로·Android App Links·iOS Universal Links 설정은 링크 연결 구현 단계에서 정한다. Supabase 프로젝트 URL을 초대 링크 도메인으로 간주하지 않는다.

## 초대 확인 계약

### preview_household_invitation_v1 — 상대 확인

요청:

```json
{"p_token": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
```

- 익명 인증 세션이 필요하다. 프로필을 아직 저장하지 않은 사용자도 확인은 가능하다.
- 앱은 허용된 링크에서 토큰을 추출해 전달한다. 서버는 전체 URL 대신 토큰만 받고 형식·해시·처리 상태·현재 공간을 확인한다.
- token은 발급 시 인코딩의 정규형만 허용한다. 공백·padding·다른 길이·32바이트가 아닌 디코딩 결과·다른 정규형은 `INVALID_ARGUMENT`이다. DB 조회 해시는 전체 링크나 토큰 문자열이 아닌 복원한 원본 32바이트의 SHA-256이다.
- 확인은 읽기 작업이며 자리를 예약하거나 초대를 사용 처리하지 않는다.

유효한 초대의 성공 `data`:

```json
{
  "invitation_id": "00000000-0000-4000-8000-000000000201",
  "household_id": "00000000-0000-4000-8000-000000000301",
  "inviter": {"display_name": "수찬", "emoji_code": "CAT"},
  "expires_at": "2026-11-27T03:00:00Z"
}
```

실패에는 상대 프로필·공간 ID·토큰 해시를 넣지 않는다. 미사용 초대라도 발급자가 해당 준비 공간의 구성원이 아니거나 공간이 닫혔다면 유효한 것으로 반환하지 않는다.

업무 오류: `INVALID_ARGUMENT`, `INVITATION_NOT_FOUND`, `SELF_INVITATION`, `INVITATION_USED`, `INVITATION_REVOKED`, `INVITATION_EXPIRED`, `HOUSEHOLD_FULL`, `ALREADY_CONNECTED`, `CURRENT_HOUSEHOLD_HAS_DATA`.

확인 성공 후에도 수락 전 재발급·만료·다른 사람의 참여가 발생할 수 있다. 앱은 확인 성공을 수락 성공으로 취급하지 않는다.

## 초대 수락 계약

### accept_household_invitation_v1 — 참여 확정

요청은 `preview_household_invitation_v1`과 같은 `p_token` 객체다. 사용자가 확인 화면의 참여 버튼을 눌렀을 때만 호출한다. 토큰을 확인했다는 이유로 자동 수락하지 않는다.

- 자신의 저장된 프로필이 필요하다.
- 앱이 전달한 공간 ID·발급자 ID를 수락 권한의 근거로 사용하지 않는다.
- 잠금 뒤 현재 서버 시각·토큰·현재 구성원·본인 여부를 다시 검사한다.
- 기존 정책에 따라 데이터 없는 1인 준비 공간만 정리하고, 구성원 추가·해당 초대 사용 완료·관련 초대 무효화를 하나의 트랜잭션으로 처리한다.
- 앱 네트워크 취소가 서버의 미실행·롤백을 뜻하지 않는다. 결과가 불명확하면 현재 관계를 재조회한다.

성공 `data`:

```json
{
  "outcome": "JOINED",
  "household": {
    "household_id": "00000000-0000-4000-8000-000000000301",
    "members": [
      {"user_id": "00000000-0000-4000-8000-000000000001", "display_name": "수찬", "emoji_code": "CAT", "revision": 1},
      {"user_id": "00000000-0000-4000-8000-000000000002", "display_name": "지영", "emoji_code": "RABBIT", "revision": 1}
    ]
  }
}
```

| outcome | 의미 |
| --- | --- |
| `JOINED` | 이번 요청으로 참여를 확정했다. |
| `ALREADY_JOINED` | 이 초대를 본인이 이미 수락했고 현재도 같은 공간에 속한다. 변경 없이 기존 참여 결과를 반환한다. |
| `ALREADY_CONNECTED` | 서로 초대를 수락하는 경합 등으로, 호출자와 해당 초대 발급자가 이미 같은 열린 공간의 두 구성원이다. 현재 공간을 반환하며 두 번째 이동을 하지 않는다. |

수락·확인의 판정 우선순위:

1. 인증과 토큰 형식 확인 후 토큰 해시로 초대를 찾는다. 없으면 `INVITATION_NOT_FOUND`다.
2. 본인 초대는 거절한다.
3. 수락 함수는 사용 완료 기록·현재 관계를 검사해 `ALREADY_JOINED`를 판정한다. 이어서 같은 두 사람의 현재 연결이 있으면 `ALREADY_CONNECTED`를 반환할 수 있다. 무효화·만료된 옛 초대도 **새 권한을 부여하지 않고 이미 존재하는 본인 관계만 반환하는 경우**다.
4. 확인 함수에서 같은 두 사람의 연결을 발견하면 업무 오류 `ALREADY_CONNECTED`로 응답해 현재 상태 조회로 안내한다. 옛 초대의 상대 정보를 다시 노출하지 않는다.
5. 그 외에는 사용 완료 → 무효화 → 만료 순서로 거절한다. 이미 다른 공간의 두 사람 연결에 속한 호출자는 `ALREADY_CONNECTED`로 거절한다. 이미 연결된 상대에게서 구성원 정보를 추가로 노출하지 않는다.
6. 공간 닫힘·발급자 소속 변경은 `INVITATION_REVOKED`로 거절한다. 대상 정원과 호출자의 준비 공간 정리 가능 여부를 검사한다.
7. 수락은 프로필을 확인한 뒤 관계를 변경한다. 초대 확인·수락 모두 같은 검증 의미를 유지하고, 수락은 잠금 뒤 재검증한다.

같은 사용자·같은 토큰의 재시도는 중복 구성원을 생성하지 않는다. 다른 사용자가 사용한 토큰으로 본인의 관계를 바꾸거나 이전 공간을 다시 열지 않는다. 이미 연결된 공간이 다른 사람과의 공간이면 성공 응답을 반환하지 않는다.

업무 오류: 확인 함수의 오류 + `PROFILE_REQUIRED`. `ALREADY_CONNECTED`는 위 성공 조건과 달리 **다른 사람과 이미 연결된 경우** 실패 envelope의 코드로 사용한다. `outcome`과 `error.code`를 같은 필드처럼 해석하지 않는다.

## 오류와 앱 동작

| 코드 | 의미 | 앱 처리 |
| --- | --- | --- |
| `INVALID_ARGUMENT` | 입력 규칙 위반 | `field`에 해당하는 입력 안내. 원본 토큰을 문구에 넣지 않는다. |
| `PROFILE_REQUIRED` | 프로필 미저장 | 내 이름·이모지 설정으로 이동한 뒤 명시적 동작으로 재개. |
| `PROFILE_CONFLICT` | 다른 프로필 값이 먼저 저장됨 | 최신 프로필 조회. 초안을 자동 덮어쓰기하지 않는다. |
| `INVITATION_NOT_FOUND` | 일치하는 초대 없음 | 링크 확인 안내. 상대 정보 표시 금지. |
| `SELF_INVITATION` | 본인이 발급한 링크 | 상대에게 보낼 링크임을 안내. |
| `INVITATION_USED` | 다른 사용자가 사용한 초대 | 사용 완료 안내. |
| `INVITATION_REVOKED` | 재발급·관계 변경으로 무효화됨 | 새 링크 요청 안내. 만료와 구분. |
| `INVITATION_EXPIRED` | 24시간 만료 | 새 링크 요청 안내. |
| `HOUSEHOLD_FULL` | 대상 공간에 빈 자리 없음 | 참여 불가 안내. |
| `ALREADY_CONNECTED` | 이미 연결된 관계가 있음 | 자신의 현재 상태 재조회. 기존 연결을 유지. |
| `CURRENT_HOUSEHOLD_HAS_DATA` | 현재 준비 공간에 서버 업무 데이터가 있음 | 자동 정리·병합 금지. 별도 정책이 필요함을 안내. |
| `ISSUANCE_RESULT_UNAVAILABLE` | 같은 발급 요청이 이미 커밋됨 | 자동 재발급 금지. 현재 상태 확인 후 필요하면 명시적으로 재발급. |

`INVALID_ARGUMENT.field`는 `display_name`, `emoji_code`, `expected_revision`, `request_id`, `token` 중 해당 항목이다. 나머지는 null이다.

위 코드는 **앱이 제안하는 업무 계약**이며 Supabase 기본 오류 코드가 아니다. 알 수 없는 업무 코드·서버 무결성 오류·응답 파싱 실패를 성공이나 빈 상태로 변환하지 않는다.

전송 계층 처리:

- HTTP 401: 같은 사용자의 세션을 갱신할 수 있는지 확인한다. 새 익명 사용자 생성으로 우회하지 않는다. 재요청 시 발급 요청 ID·수락 토큰을 바꾸지 않는다.
- HTTP 403: 권한 오류로 안내한다. 가입 성공이나 초대 없음으로 변환하지 않는다.
- HTTP 429: 제한 안내 후 재시도한다. 서버가 Retry-After를 제공하면 반영한다. 새 발급 ID를 자동 생성하지 않는다.
- HTTP 5xx·timeout·연결 끊김: 변경 요청은 이미 커밋됐을 수 있다. 결과 불명확 상태를 표시하고 조회로 확인한다.
- `CancellationException`: 일반 오류로 바꾸지 않는다. 화면 이탈 뒤 오래된 결과가 새 화면을 덮어쓰지 않도록 한다.

## 재시도·상태 복원 요약

| 작업 | 결과를 잃었을 때 |
| --- | --- |
| 현재 상태·초대 확인 | 같은 입력으로 재조회 가능. 최신 상태가 달라질 수 있다. |
| 프로필 저장 | 같은 입력·expected revision으로 재시도 가능. 현재 값이 같으면 성공, 다르면 충돌을 처리한다. |
| 초대 발급 | 같은 ID의 부수효과 중복만 차단. 현재 상태 확인 후 사용자 동의로 새 ID 재발급. |
| 초대 수락 | 우선 현재 관계 조회. 필요하면 같은 토큰으로 재시도하며 이미 확정된 연결을 보존. |

- 앱 재실행 시 서버 관계를 조회한다. 메모리의 `isConnected`만으로 복원하지 않는다.
- 서버 조회 실패 때 `NOT_CONNECTED`로 초기화하지 않는다. 마지막 확인 상태와 갱신 실패를 구분한다.
- 앱에 보관한 발급 링크는 서버가 알려준 최신 invitation ID와 일치하고 유효할 때만 공유 가능하다. 다른 초대 발급·만료·연결 완료를 확인하면 공유를 막는다.
- 상대가 초대를 수락한 뒤 발급자 화면에 반영하는 방식은 이후 앱 연결에서 정의한다. 현재 상태 조회가 기준이며 Realtime 구독은 아직 계약의 필수 조건이 아니다.
- 임의 URL을 열거나 받은 URL의 호스트로 API 요청을 보내지 않는다. 허용된 앱 링크만 해석하고, 확인·수락은 설정된 Supabase 프로젝트로만 요청한다.

## KMP 계층 연결 기준

- `core:data`: RPC 요청·응답 DTO, SDK 호출, envelope·전송 오류 해석과 Mapper.
- `core:domain`: 내 프로필·현재 공간·발급 결과·초대 확인·참여 결과의 모델과 Repository 계약. SDK·JSON·UI 문구는 노출하지 않는다.
- `feature:household`: 로딩·입력·오류·확인·연결 완료 상태 전이. 프로필 저장 후에만 다음 서버 작업으로 진행한다.
- `shared`: DI 조립, 허용된 초대 URL 해석·생성, 플랫폼 링크·공유 연결.

domain 결과는 기존 `AppResult<T>`를 사용한다. 현재 `AppError` 네 종류만으로는 위 업무·인증·통신 오류를 구분할 수 없으므로 구현 단계에서 필요한 오류 계약을 확장하고 기존 exhaustive mapper도 검토한다. 모든 서버 실패를 `Storage`로 일괄 변환하지 않는다. 현재 익명 인증 구현이 이미 이 계약 전체를 제공하는 것으로 간주하지 않는다.

## 로컬 SQL 검증 결과

1~3차 SQL을 임시 네이티브 PostgreSQL 17.12에 적용했다. 테스트 환경의 Auth 테이블·uid 함수·역할은 최소 모형이며 운영 Supabase에 연결하지 않았다. pgcrypto 난수 생성과 SHA-256 저장 검사는 실제 확장을 사용했다.

- 24시간 만료, 정규형 토큰, 프로필 필요 여부, 재발급·옛 링크 차단, 중복 발급 요청·수락 재시도 확인.
- 서로 초대한 경우의 준비 공간 정리, 서버 업무 데이터가 있는 공간 보존 확인.
- 중간 오류 주입 시 구성원·공간 폐쇄·초대 무효화 전체 롤백 확인.
- 별도 DB 연결에서 서로 다른 수락자, 동일 수락자의 다른 링크, 두 사람의 상호 수락, 같은 요청 ID 발급을 동시에 실행해 결과 확인.
- 실제 잠금 대기 중 만료 시각을 지나도록 한 뒤 수락 거절 확인.
- 미인증 RPC·비공개 헬퍼·초대 원본 테이블 접근 차단 확인.

Supabase JWT 검증·PostgREST 경유 응답·실제 Android/iOS 연결은 이 로컬 SQL 검증에 포함되지 않는다.

## 다음 구현과 검증

1. 초대 발급·확인·수락의 앱 Repository·화면을 연결한다.
2. 실제 Supabase JWT·API로 타 사용자 접근 차단, 만료·발급 결과 유실·수락 재시도 처리를 확인한다.
3. 초대 URL·공유·Android/iOS 링크 진입을 구현하고 실제 두 사용자의 연결·재실행 복원을 검증한다.

이 목록은 향후 검증 기준이며 이번 문서 작업에서 실행한 결과가 아니다.

## 기술 참고

- [Supabase Kotlin RPC](https://supabase.com/docs/reference/kotlin/rpc): PostgreSQL 함수 호출 방법.
- [Supabase Database Functions](https://supabase.com/docs/guides/database/functions): 서버 함수와 실행 권한.
- [PostgREST 오류](https://docs.postgrest.org/en/stable/references/errors.html): 게이트웨이·PostgreSQL 오류의 기본 응답 형식. 이번 업무 envelope와 구분한다.
