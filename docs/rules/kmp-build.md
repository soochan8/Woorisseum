# KMP 플랫폼 경계와 빌드 규칙

## 적용 범위

플랫폼 API, 소스셋, Gradle 설정, 라이브러리·모듈 의존성 또는 아키텍처 검사기를 변경할 때 읽는다.

## 공통 코드와 플랫폼 연결

- 비즈니스 로직과 공통 Compose UI는 `commonMain`에 둔다. Android와 iOS에 별도 MVI 로직을 중복 구현하지 않는다.
- Android API 연결은 `androidMain`, iOS API 연결은 `iosMain`에 둔다.
- 플랫폼 차이가 실제로 존재하는 경계에 인터페이스 또는 필요한 `expect/actual`을 사용한다. 플랫폼 분기가 필요 없는 타입까지 추상화하지 않는다.
- Android 전용 라이브러리·타입을 `commonMain`의 의존성이나 공개 계약에 넣지 않는다.
- 공통 모듈은 Android·iOS arm64·iOS Simulator arm64 타깃을 유지한다.
- iOS 정적 `Shared` 프레임워크는 shared에서만 생성한다. 내부 모듈은 필요한 경우에만 Swift API로 export한다.
- `iosApp`은 Xcode 호스트다. Gradle 프로젝트 모듈처럼 취급하지 않고 기존 공통 Compose 연결을 유지한다.

## Gradle와 의존성

- 라이브러리 버전과 좌표는 `gradle/libs.versions.toml`에서 관리한다.
- 각 모듈이 사용하는 라이브러리와 프로젝트 의존성은 해당 `build.gradle.kts`에 명시한다.
- 반복 타깃·컴파일러·플러그인 설정은 `build-logic`의 convention plugin에서 관리한다.
- 기본은 `implementation`이다. 공개 API가 외부 타입을 노출해야 할 때만 `api`를 사용한다.
- Gradle 실행 JDK와 JVM 바이트코드 타깃은 별개다. 현재 JDK 21·바이트코드 11 설정을 근거 없이 일괄 변경하지 않는다.
- generated sources, `.gradle`, `.kotlin`, `build` 산출물을 직접 수정하거나 커밋하지 않는다.
- 빌드 속도는 필요한 타깃 실행과 기존 병렬·빌드 캐시·configuration cache를 우선 활용한다. 기본 활성화 옵션을 성능 개선처럼 중복 선언하지 않는다.
- 개발 중 불필요한 `clean`, 전체 `build`·`assemble`을 반복하지 않는다. 성능은 같은 타깃·변경·캐시 조건에서 비교한다.
- 호환성을 확인하지 않은 실험적 옵션, 로컬 장비에만 맞는 worker·메모리 값을 공통 설정에 추가하지 않는다.

## moduleRules와 Baseline

- 실제 허용 규칙: `build-logic/src/main/kotlin/com/woorisseum/buildlogic/ModuleRules.kt`.
- 승인된 기존 위반: 루트 `module-rules.txt`. 설명 주석과 빈 줄은 비교에서 제외한다.
- 검사기 명령은 `./gradlew moduleRules`, 루트 검증은 `./gradlew :check`다. 둘을 중복 실행할 필요는 없다.
- `./gradlew moduleRulesBaseline`은 검토한 예외를 반영하거나 해결된 기존 예외를 제거할 때만 실행한다. 검사 통과를 위해 자동 갱신하지 않는다.
- 새 위반은 `+`, 해결된 기존 위반은 `-`로 표시된다. 후자는 오래된 Baseline 정리가 필요하다는 뜻이다.
- 새 모듈을 추가하면 허용 규칙에도 명시한다. 미분류 모듈·순환 의존은 Baseline으로 승인하지 않는다.
- 자동 검사는 직접 프로젝트 의존과 domain 의존성·플러그인 일부를 확인한다. MVI·SOLID·화면 동작을 전부 보장한다고 설명하지 않는다.
- 현재 요청과 무관한 사용자 변경·테스트용 위반은 임의 수정하지 않고, 변경으로 생긴 문제와 구분해 보고한다.

## 변경 후 확인

변경에 필요한 작업만 실행한다. 테스트 코드 추가·수정 여부는 루트 AGENTS.md의 현재 작업 지침을 따른다.

```bash
# 모듈 경계·domain 의존성 변경
./gradlew moduleRules

# Android 빌드 설정·공통 Android 코드 변경
./gradlew :androidApp:assembleDebug

# iOS Simulator 설정·플랫폼 코드 변경
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

문서만 바꿨다면 링크와 규칙의 일관성을 확인하고 앱 전체를 빌드하지 않는다.
