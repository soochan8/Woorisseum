# 우리씀 공통 디자인 시스템

## 현재 범위

Android·iOS가 `core:designsystem`의 공통 Compose 구현을 사용한다. feature·domain·data에 의존하지 않는다.
앱 전체에 청록·따뜻한 흰색의 라이트 팔레트와 대응하는 다크 팔레트를 적용한다. 기존 화면의 버튼·입력창·필터는 공통 컴포넌트를 사용한다. 화면의 정보 배치와 기능별 MVI 계약은 유지한다. 글자는 기본 글꼴을 사용하되 현재 사용하는 Material 타이포그래피 역할별 크기·줄 높이·굵기를 정의했다. 기본 모양은 Material 기준이다.

## 테마와 간격

- 앱 진입점과 Preview를 `WoorisseumTheme`으로 감싼다.
- 색상은 `theme/Color.kt`, 글꼴은 `theme/Typography.kt`, 모양은 `theme/Shape.kt`에서 관리한다.
- 화면은 `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`를 사용한다. 각 파일의 내부 값에 직접 의존하지 않는다.
- 공통 간격은 `WoorisseumSpacing`의 4·8·12·16·24dp와 `screenHorizontal`을 사용한다. 기능 전용 치수까지 공통 토큰으로 만들지 않는다.
- 사용자 글자 크기를 존중한다. 고정 높이 대신 필요한 최소 높이를 사용한다.
- 임시 컨트롤 테마를 제거하고 `Color.kt`의 팔레트를 `WoorisseumTheme`에서 한 번 제공한다. 컴포넌트마다 MaterialTheme을 다시 감싸거나 화면별 팔레트를 만들지 않는다.
- 주요·보조·텍스트 버튼은 `shapes.medium`(현재 12dp), 입력창·필터는 `shapes.small`(현재 8dp)을 사용한다. 화면에서 모양을 반복 선언하지 않는다.
- 청록은 실행·선택, 옅은 회녹색은 선택 배경에 사용한다. 벽돌색은 보조 브랜드 색상으로 준비했으나 오류나 삭제 의미에 사용하지 않는다. 오류·비활성 상태는 Material의 의미와 동작을 유지한다.
- 다크 모드는 검정에 가까운 배경 대신 차콜 회녹색을 사용한다. 배경 `#282E2B`, 기본 표면 `#303733`, 상위 표면 `#3D463F`로 구분하고 보조 글자·구분선도 함께 밝힌다.

## 공통 타이포그래피

스타일은 `MaterialTheme.typography`에서 가져온다. 기본 글꼴은 `FontFamily.Default`이며 Android·iOS에서 실제 글꼴과 글리프 폭은 다를 수 있다. 별도 폰트·라이브러리는 추가하지 않는다.

| 용도 | 스타일 | 크기 / 줄 높이(sp) | 굵기 |
| --- | --- | --- | --- |
| 핵심 합계·금액 | `displaySmall` | 32 / 40 | SemiBold |
| 큰 강조 제목 | `headlineMedium` | 24 / 32 | SemiBold |
| 화면 제목 | `headlineSmall` | 22 / 30 | SemiBold |
| 시트·대화상자 제목 | `titleLarge` | 20 / 28 | SemiBold |
| 섹션 제목 | `titleMedium` | 16 / 24 | SemiBold |
| 작은 묶음 제목 | `titleSmall` | 15 / 22 | Medium |
| 사용처·물품명·입력·본문 | `bodyLarge` | 16 / 24 | Regular |
| 설명 문장 | `bodyMedium` | 14 / 22 | Regular |
| 날짜·결제수단·보조 정보 | `bodySmall` | 13 / 20 | Regular |
| 버튼·필터 | `labelLarge` | 15 / 22 | Medium |
| 짧은 보조 라벨 | `labelMedium` | 13 / 18 | Medium |
| 작은 분류 표시 | `labelSmall` | 12 / 16 | Medium |
| 핵심 금액의 통화 단위 | `amountUnit` 확장 스타일 | 22 / 30 | Regular |
| 목록의 금액 열 | `listAmount` 확장 스타일 | 16 / 24 | Medium |

- 자간은 0sp로 두고 한글을 일괄 압축하지 않는다. 크기·줄 높이 모두 sp로 사용자 글자 크기를 존중한다.
- 핵심 금액과 목록 금액에 `tnum`을 지정한다. 지원 글꼴에서 숫자 폭을 일정하게 하는 옵션이며 금액 포맷·통화 단위·우측 정렬을 대신하지 않는다.
- `amountUnit`과 `listAmount`는 `Typography`의 확장 속성이다. 각각 제목·본문 스타일에서 파생하므로 별도의 테마나 상태는 만들지 않는다. 사용 시 확장 속성을 import한 뒤 `MaterialTheme.typography.amountUnit` 등으로 접근한다.
- 텍스트 색상은 사용 맥락에 맞춰 `colorScheme`에서 선택한다. TextStyle에 오류색이나 특정 기능의 색을 넣지 않는다.
- 역할에 맞는 스타일을 선택하고 화면마다 크기·자간·굵기를 반복 지정하지 않는다. 선택 상태 강조 등 필요한 예외만 해당 화면에서 결정한다.
- 지출·예산·고정지출·카드·장보기·관리의 화면 제목은 `headlineSmall`을 명시한다. 지출 합계의 38sp·음수 자간과 중복 SemiBold는 제거했다. 사용처·물품명은 `bodyLarge`, 지출 목록의 금액은 `listAmount`, 지출·예산 합계의 통화 단위는 `amountUnit`을 사용한다. 선택한 분류 탭의 굵기 차이는 상태 표현으로 유지한다.
- 텍스트 크기를 맞추려고 고정 높이·최대 줄 수를 새로 강제하지 않는다. 긴 금액과 큰 글씨는 화면별 줄바꿈·가용 폭도 함께 확인해야 한다.
- 사용하지 않는 큰 display 역할은 Material 기본값을 유지한다. 실제 용도가 생기면 기준을 정한다.

## 공통 컴포넌트

| 컴포넌트 | 책임 |
| --- | --- |
| `WoorisseumPrimaryButton` | 테마 기반 주요 실행 버튼과 최소 높이 48dp. 아이콘·문자열·활성 상태·클릭 처리는 호출자가 전달 |
| `WoorisseumSecondaryButton` | 외곽선으로 구분하는 보조 실행 버튼, 최소 높이 48dp |
| `WoorisseumTextButton` | 취소·닫기 등 강조를 낮춘 동작, 최소 높이 48dp |
| `WoorisseumTextField` | 공통 포커스 색상과 입력 모양. 입력값·오류·문구·키보드 정책은 호출자가 전달 |
| `WoorisseumFilterChip` | 선택·미선택·비활성 필터의 공통 표현. 카테고리 정책은 feature 소유 |
| `WoorisseumBottomActionBar` | 하단 실행 영역의 배경, 공통 여백, 시스템 내비게이션 바 inset. 여러 버튼이나 입력 영역도 슬롯으로 배치 |
| `SwipeToRevealDeleteRow` | 스와이프로 삭제 버튼 노출, 명시적인 삭제 콜백과 접근성 동작 |

`WoorisseumBottomActionBar`는 Scaffold의 `bottomBar`에 사용한다. 내부 콘텐츠에 내비게이션 바 inset을 중복 적용하지 않는다.
전체 화면의 safeDrawing, 키보드 처리와 스크롤은 화면이 소유한다. 공통 컴포넌트가 ViewModel·Intent·업무 검증을 알지 않게 한다.
앱 문자열은 호출하는 기능의 Compose Resources에서 전달한다. 예시 문구는 Preview에만 둔다.

공통 컴포넌트는 Material 동작·접근성 위에 앱의 표현 기준을 적용한다. 모든 Material API를 그대로 복제하지 않는다.
지출·검색·고정지출·카드·예산·장보기 화면과 대화상자의 버튼·입력창·필터에 연결했다. 검색창의 지우기 아이콘, 키보드 동작, 여러 줄 메모, 비활성·오류 상태는 화면이 전달한다. 하단 액션 영역과 화면 좌우 간격도 공통 기준을 사용한다. 스와이프 삭제는 오류색을 사용하는 기존 전용 컴포넌트를 유지한다.
앱 바 등 스타일이 확정되지 않은 요소는 Material 컴포넌트를 유지한다.
기능 전용 폼, 금액 표시 변환, 결제수단·카테고리 정책은 feature에 유지한다.

## 확인

`component/DesignSystemPreview.kt`에서 라이트·다크·320dp/큰 글씨, 버튼·입력·필터의 기본·선택·오류·비활성 상태와 하단 영역을 확인한다. 입력과 필터는 Interactive Preview에서 조작할 수 있다.
새 시각 스타일은 먼저 이 Preview에서 확인하고, 실제 화면에 적용한다. 접근성·레이아웃 기준은 [Compose 규칙](rules/compose.md)을 따른다.
