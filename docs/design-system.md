# 우리씀 공통 디자인 시스템

## 현재 범위

Android·iOS가 `core:designsystem`의 공통 Compose 구현을 사용한다. feature·domain·data에 의존하지 않는다.
앱 전체에 청록·따뜻한 흰색의 라이트 팔레트와 대응하는 다크 팔레트를 적용한다. 기존 화면의 버튼·입력창·필터는 공통 컴포넌트를 사용한다. 화면의 정보 배치와 기능별 MVI 계약은 유지한다. 글꼴·기본 모양은 Material 기준이다.

## 테마와 간격

- 앱 진입점과 Preview를 `WoorisseumTheme`으로 감싼다.
- 색상은 `theme/Color.kt`, 글꼴은 `theme/Typography.kt`, 모양은 `theme/Shape.kt`에서 관리한다.
- 화면은 `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`를 사용한다. 각 파일의 내부 값에 직접 의존하지 않는다.
- 공통 간격은 `WoorisseumSpacing`의 4·8·12·16·24dp와 `screenHorizontal`을 사용한다. 기능 전용 치수까지 공통 토큰으로 만들지 않는다.
- 사용자 글자 크기를 존중한다. 고정 높이 대신 필요한 최소 높이를 사용한다.
- 임시 컨트롤 테마를 제거하고 `Color.kt`의 팔레트를 `WoorisseumTheme`에서 한 번 제공한다. 컴포넌트마다 MaterialTheme을 다시 감싸거나 화면별 팔레트를 만들지 않는다.
- 주요·보조·텍스트 버튼은 `shapes.medium`(현재 12dp), 입력창·필터는 `shapes.small`(현재 8dp)을 사용한다. 화면에서 모양을 반복 선언하지 않는다.
- 청록은 실행·선택, 옅은 회녹색은 선택 배경에 사용한다. 벽돌색은 보조 브랜드 색상으로 준비했으나 오류나 삭제 의미에 사용하지 않는다. 오류·비활성 상태는 Material의 의미와 동작을 유지한다.

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
