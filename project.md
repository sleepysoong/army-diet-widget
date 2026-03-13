# Project Analysis

이 문서는 이 저장소를 빠르게 다시 이해하기 위한 상세 컨텍스트 문서다.
세션 compact 이후나 새 세션에서 프로젝트를 다시 파악할 때 우선 참고하는 문서로 사용한다.

- Last reviewed: 2026-03-13
- Repository: `army-diet-widget`
- Platform: Android
- Main language: Kotlin

## 1. 이 프로젝트가 하는 일

이 프로젝트는 군 식단을 보여주는 안드로이드 앱이자 홈 화면 위젯이다.

기본 모드는 국방부 공공데이터 포털 API에서 식단을 받아 로컬 DB에 저장한 뒤 앱과 위젯에서 공통으로 보여준다.
추가로, 사용자가 별도 외부 API 엔드포인트를 입력하면 그 서버를 데이터 소스로 사용할 수도 있다.

사용자 관점의 핵심 기능은 아래와 같다.

- 오늘 식단 확인
- 홈 화면에서 이전/다음 날짜로 이동
- 18시 이후에는 자동으로 내일 식단 표시
- 홈 화면 위젯으로 식단 확인
- 위젯 글자 크기/태그 크기/헤더 크기/칼로리 표시 여부 설정
- 캘린더 기반 날짜별 식단 조회
- 캘린더 화면에서 식단 직접 수정
- "맛있는 음식" 키워드 강조 표시
- 설정 화면에서 데이터 소스 전환
- 설정 화면에서 국방부 부대 코드(기본 `7369`) 변경
- 설정 화면에서 외부 API 엔드포인트 저장

짧게 말하면 "군 식단 캐시 앱 + Compose UI + Glance 위젯 + 선택 가능한 데이터 소스" 구조다.

## 2. 기술 스택

`app/build.gradle.kts` 기준 주요 스택은 아래와 같다.

- Android Gradle Plugin `8.2.0`
- Kotlin Android `1.9.22`
- Jetpack Compose
- Material3
- Room
- DataStore Preferences
- Retrofit + Gson Converter
- OkHttp + Logging Interceptor
- WorkManager
- Kotlin Coroutines
- Glance AppWidget

버전 관리는 `version.properties`를 통해 분리되어 있으며,
GitHub Actions가 `main` 브랜치 push 시 `VERSION_CODE` / `VERSION_NAME`을 자동으로 올린 뒤 APK를 빌드하도록 구성되어 있다.

빌드 타깃 관련 설정은 다음과 같다.

- `compileSdk = 34`
- `targetSdk = 34`
- `minSdk = 26`
- Compose 활성화
- KAPT 사용(Room 컴파일러)

## 3. 전체 구조

루트 구조는 대략 아래와 같다.

- `app/` - 실제 안드로이드 앱 모듈
- `gradle/` - Gradle wrapper 설정
- `scripts/` - 로컬 훅/보조 스크립트
- `.github/workflows/` - CI 워크플로우
- `screenshots/` - README용 이미지

앱 코드 구조는 `app/src/main/java/com/sleepysoong/armydiet` 아래에 정리되어 있다.

- `data/local/` - Room, DataStore 등 로컬 저장 계층
- `data/remote/` - Retrofit API/DTO/네트워크 생성기
- `domain/` - 저장소 조합과 동기화 로직
- `di/` - 수동 DI 컨테이너
- `ui/` - Compose 화면과 ViewModel
- `ui/components/` - 재사용 UI 조각
- `ui/theme/` - 테마, 색상, 타이포그래피
- `widget/` - Glance 위젯, 위젯 설정, 리시버
- `worker/` - 백그라운드 동기화 워커

## 4. 런타임 진입점과 앱 시작 흐름

### 4.1 애플리케이션 시작

앱의 `Application` 클래스는 `ArmyDietApp`이다.

- 파일: `app/src/main/java/com/sleepysoong/armydiet/ArmyDietApp.kt`
- 역할: 전역 `AppContainer` 생성

`ArmyDietApp.onCreate()`에서 `AppContainer.getInstance(this)`를 호출해 전역 컨테이너를 준비한다.

### 4.2 메인 액티비티

앱의 메인 진입점은 `MainActivity`다.

- 파일: `app/src/main/java/com/sleepysoong/armydiet/MainActivity.kt`
- 역할:
  - `enableEdgeToEdge()` 적용
  - 주기적 동기화용 `SyncWorker` 등록
  - Compose `AppTheme` 적용
  - `MainViewModel` 생성 후 `MainScreen` 표시

`MainActivity.scheduleSyncWorker()`는 다음 조건으로 주기 워커를 등록한다.

- 네트워크 연결 필요
- 12시간 주기
- backoff: exponential, 30분
- 중복 등록 방지: `enqueueUniquePeriodicWork(..., ExistingPeriodicWorkPolicy.KEEP, ...)`

## 5. 아키텍처 요약

이 프로젝트는 엄격한 클린 아키텍처나 DI 프레임워크 기반 구조는 아니고,
"작은 앱에 맞춘 수동 레이어 분리"에 가깝다.

핵심 관계는 아래와 같다.

1. `ArmyDietApp`이 `AppContainer`를 준비한다.
2. `AppContainer`가 DB, DAO, Preferences, Retrofit API, Repository를 lazy로 만든다.
3. `MainViewModel`이 `MealRepository`와 `AppPreferences`를 사용해 UI 상태를 만든다.
4. 앱 화면과 위젯이 모두 `MealRepository`/`Room`/`DataStore`를 공통으로 사용한다.

이 구조의 장점은 단순함이다.
대신 테스트 더블 주입이나 모듈 교체는 Hilt/Koin 기반 구조보다 불편하다.

## 6. DI와 객체 생성 방식

DI는 `AppContainer` 하나로 처리한다.

- 파일: `app/src/main/java/com/sleepysoong/armydiet/di/AppContainer.kt`

`AppContainer`가 제공하는 주요 객체:

- `AppDatabase`
- `MealDao`
- `MndApi`
- `AppPreferences`
- `MealRepository`

특징:

- 앱 컨텍스트 기반 싱글톤
- 모든 주요 객체를 `lazy`로 생성
- 외부 API는 `MealRepository` 생성 시 `(String) -> ExternalMealApi` 팩토리 람다로 전달

즉, 이 프로젝트는 "간단한 수동 Service Locator + lazy singleton" 패턴을 쓴다.

## 7. 데이터 소스와 데이터 흐름

### 7.1 공통 개념

앱/위젯 모두 최종적으로는 `MealRepository.getMeal(date)`를 통해 식단을 읽는다.
리포지토리는 로컬 DB를 단일 조회 원천처럼 사용하고, 필요할 때만 동기화를 시도한다.

### 7.2 메인 리포지토리

- 파일: `app/src/main/java/com/sleepysoong/armydiet/domain/MealRepository.kt`

`MealRepository`의 핵심 역할:

- 로컬 DB 조회
- 동기화 필요 여부 판단
- 국방부 API 배치 동기화
- 외부 API 단건 동기화
- 메뉴 텍스트 정리
- 기존 데이터와 신규 데이터 병합

내부 구현상 중요한 포인트:

- `syncMutex`로 동기화 중복 실행 방지
- 마지막 동기화 시각 기준 24시간 간격 체크
- 로컬 모드에서는 `lastCheckedIndex`를 이용한 증분 동기화
- 전체 초기 동기화와 증분 동기화의 최대 아이템 수 분리
- 날짜 문자열을 여러 포맷(`yyyy-MM-dd`, `yyyy.MM.dd`, `yyyyMMdd`)으로 파싱
- 알레르기 표기 정규식 `\([0-9.]+\)` 제거

### 7.3 로컬 모드(MND API)

국방부 API 연동은 `MndApi`와 DTO가 담당한다.

- `app/src/main/java/com/sleepysoong/armydiet/data/remote/MndApi.kt`
- `app/src/main/java/com/sleepysoong/armydiet/data/remote/MndDto.kt`

API 경로는 아래 형태다.

- `/{apiKey}/json/DS_TB_MNDT_DATEBYMLSVC_{부대코드}/{startIndex}/{endIndex}/`

현재는 설정 화면에서 `DS_TB_MNDT_DATEBYMLSVC_` 뒤에 붙는 부대 코드를 바꿀 수 있다.
기본값은 `7369`다.

동기화 흐름은 대략 이렇다.

1. `performSync()`가 1건 요청으로 전체 건수 조회
2. `lastCheckedIndex` 이후부터 배치 크기 `1000`으로 반복 요청
3. 응답 row를 날짜별로 그룹화
4. 날짜별 `MealEntity`로 fold
5. 기존 DB 데이터와 병합
6. Room에 upsert(REPLACE)
7. 마지막 처리 index/timestamp 저장

`MealEntity.merge()` 로직 때문에 같은 날짜의 식단 항목은 단순 덮어쓰기가 아니라 메뉴 문자열을 합쳐 정렬/중복 제거하는 방향으로 동작한다.

### 7.4 외부 API 모드

외부 API는 아래 인터페이스를 따른다.

- 파일: `app/src/main/java/com/sleepysoong/armydiet/data/remote/ExternalMealApi.kt`

응답 구조:

```json
{
  "success": true,
  "data": {
    "date": "20260313",
    "breakfast": ["..."],
    "lunch": ["..."],
    "dinner": ["..."],
    "total_calories": "..."
  },
  "error": null
}
```

주요 엔드포인트:

- `GET api/menu/today`
- `GET api/menu?date=yyyyMMdd&meal=...`

실제 리포지토리에서는 `getMenu(date)`를 사용한다.

`NetworkModule.createExternalApi(baseUrl)`는 base URL 끝의 `/`를 강제로 정규화한다.

### 7.5 로컬 DB를 소스 오브 트루스로 쓰는 방식

이 프로젝트는 원격 데이터를 바로 UI에 바인딩하지 않는다.
항상 Room을 중간 저장소로 사용한다.

즉,

- 원격 API -> `MealRepository` -> Room 저장
- UI/위젯 -> Room 기반 조회

이 구조 덕분에 앱과 위젯이 같은 데이터를 공유하고,
오프라인 캐시처럼 동작할 수 있다.

## 8. 로컬 저장 구조

### 8.1 Room

주요 파일:

- `app/src/main/java/com/sleepysoong/armydiet/data/local/MealEntity.kt`
- `app/src/main/java/com/sleepysoong/armydiet/data/local/MealDao.kt`
- `app/src/main/java/com/sleepysoong/armydiet/data/local/AppDatabase.kt`

`MealEntity` 필드:

- `date`
- `breakfast`
- `lunch`
- `dinner`
- `adspcfd`
- `sumCal`
- `updatedAt`

DAO가 제공하는 주요 동작:

- 날짜 단건 조회
- 여러 날짜 일괄 조회
- 날짜 범위 조회
- 전체 식단 Flow 조회
- 다건 insert(REPLACE)

중요한 점:

- DB 버전은 `2`
- `fallbackToDestructiveMigration()` 사용

즉, 스키마 변경 시 기존 데이터가 삭제될 수 있다.

### 8.2 앱 설정 DataStore

파일:

- `app/src/main/java/com/sleepysoong/armydiet/data/local/AppPreferences.kt`

여기에는 아래 정보가 저장된다.

- API Key
- 마지막 동기화 index
- 마지막 동기화 timestamp
- 강조 키워드 목록
- 식단 데이터 소스(`local` / `external`)
- 국방부 부대 코드(기본 `7369`)
- 외부 API 엔드포인트

기본 강조 키워드는 육류/인기 메뉴 중심의 한국어 단어 세트다.

예시:

- `소시지`
- `닭`
- `삼겹살`
- `불고기`
- `돈가스`
- `갈비`
- `고기`

### 8.3 위젯별 설정 DataStore

파일:

- `app/src/main/java/com/sleepysoong/armydiet/widget/WidgetConfig.kt`

위젯 인스턴스별로 아래 값이 따로 저장된다.

- `fontScale`
- `tagScale`
- `headerScale`
- `showCalories`

앱 위젯 ID를 키 suffix로 붙이는 방식이라 위젯마다 서로 다른 설정을 유지할 수 있다.

## 9. 메인 UI 구조

### 9.1 메인 화면 구성

`MainScreen`은 두 탭 중심이다.

- 오늘 식단 탭
- 캘린더 탭

상단에는 헤더가 있고, 하단에는 떠 있는 네비게이션 바가 있다.

관련 파일:

- `app/src/main/java/com/sleepysoong/armydiet/MainActivity.kt`
- `app/src/main/java/com/sleepysoong/armydiet/ui/CalendarScreen.kt`
- `app/src/main/java/com/sleepysoong/armydiet/ui/components/`

### 9.2 TodayScreen 상태 모델

`TodayScreen`은 `MealUiState`를 기준으로 분기한다.

주요 상태:

- 소스 선택 화면
- API Key 입력 화면
- 외부 엔드포인트 입력 화면
- 로딩 화면
- 에러 화면
- 성공 화면

즉, 메인 화면은 "데이터 보여주기" 이전에 "현재 사용 가능한 소스/자격 정보가 있는지"를 먼저 확인하는 구조다.

### 9.3 캘린더 화면

`CalendarScreen`은 반응형이다.

- 넓은 화면: 좌우 분할
- 좁은 화면: 상하 분할

입력받는 값:

- 현재 선택 날짜
- 날짜별 식단 맵
- 선택된 날짜의 식단
- 강조 키워드
- 식단 수정 콜백

메인 화면에서는 `mealDao.getAllMealsFlow()`를 collect 해서 캘린더용 전체 데이터를 만든다.

### 9.4 설정 화면

`SettingsActivity` / `SettingsScreen`에서 관리하는 것:

- 데이터 소스 선택
- 국방부 부대 코드 저장
- 외부 API 엔드포인트 저장
- 강조 키워드 추가/삭제
- API Key 리셋
- 외부 엔드포인트 리셋

국방부 부대 코드가 바뀌면 기존 Room 캐시를 비우고 sync 상태를 초기화해서,
이전 부대 식단이 남아 있지 않도록 만든다.

설정 변경 후에는 `MealWidgetReceiver.updateAllWidgets(context)`를 호출해 위젯도 같이 갱신한다.

## 10. ViewModel 동작

파일:

- `app/src/main/java/com/sleepysoong/armydiet/ui/MainViewModel.kt`

`MainViewModel`의 핵심 책임:

- 초기 상태 판단
- 저장된 설정값 확인
- 데이터 소스별 필수 입력값 검사
- 오늘/내일 대상 날짜 계산
- 홈 화면 이전/다음 날짜 탐색
- 리포지토리에서 식단 로드
- API Key/외부 엔드포인트 저장
- 위젯 갱신 트리거
- 오류 시 재설정 흐름 제공

중요한 동작 포인트:

- 앱 시작 시 즉시 `loadMeal()` 호출
- 홈 화면에서 이전/다음 날짜 이동 가능
- 기본 기준 날짜(현재 시각 기준 오늘 또는 내일)로 빠르게 복귀 가능
- 둘 다 비어 있으면 `SourceSelection`
- 외부 모드인데 endpoint 없으면 `ExternalEndpointMissing`
- 로컬 모드인데 API key 없으면 `ApiKeyMissing`
- 18시 이후면 대상 날짜를 내일로 계산

로컬 모드에서는 데이터가 없을 때 `syncAndRetry()`로 추가 동기화를 시도하지만,
외부 모드에서는 같은 방식의 재시도 분기가 상대적으로 단순하다.

## 11. 위젯 구조

주요 파일:

- `app/src/main/java/com/sleepysoong/armydiet/widget/MealWidget.kt`
- `app/src/main/java/com/sleepysoong/armydiet/widget/MealWidgetReceiver.kt`
- `app/src/main/java/com/sleepysoong/armydiet/widget/WidgetConfigActivity.kt`
- `app/src/main/java/com/sleepysoong/armydiet/widget/WidgetConfig.kt`

위젯 특징:

- Glance 기반
- Responsive size mode 사용
- 여러 위젯 크기 대응
- 앱 위젯 ID별 사용자 설정 지원
- 앱과 동일한 repository/preferences를 사용

위젯 데이터 로드 시 하는 일:

1. `GlanceId`에서 앱 위젯 ID 추출
2. `WidgetConfig` 로드
3. 대상 날짜 계산
4. `MealRepository.getMeal(date)` 호출
5. 강조 키워드와 설정값을 합쳐 `WidgetData` 구성
6. 화면 렌더링

위젯 갱신 경로:

- 앱 설정 변경 시 전체 갱신
- 메인 화면 데이터 로드 성공 시 갱신
- `SyncWorker` 성공 시 갱신
- 리시버 `onUpdate` / `onEnabled` 시 갱신

위젯 삭제 시에는 해당 위젯 ID의 설정을 정리한다.

## 12. 백그라운드 동기화

파일:

- `app/src/main/java/com/sleepysoong/armydiet/worker/SyncWorker.kt`

`SyncWorker`는 다음 순서로 동작한다.

1. `AppContainer` 획득
2. `AppPreferences`와 `MealRepository` 획득
3. API key 읽기
4. 저장된 국방부 부대 코드를 포함한 MND API 경로로 동기화 시도
5. API key 없으면 동기화 스킵 후 성공 반환
6. 동기화 성공 시 위젯 전체 갱신
7. HTTP/IO/기타 예외에 따라 `success/retry/failure` 반환

중요한 관찰점:

- 현재 워커는 API key가 없으면 바로 종료한다.
- 즉, 외부 API 모드 사용 시에는 워커가 외부 endpoint 기준 주기 동기화를 수행하지 않는다.
- 외부 모드의 자동 갱신은 현재 로컬 모드만큼 직접적이지 않다.

이게 의도인지, 아직 미완성인지, 혹은 단순한 누락인지 추후 판단이 필요하다.

## 13. 디자인 시스템과 UI 톤

관련 파일:

- `app/src/main/java/com/sleepysoong/armydiet/ui/theme/Theme.kt`
- `app/src/main/java/com/sleepysoong/armydiet/ui/theme/Color.kt`
- `app/src/main/res/font/`

디자인 방향:

- Apple-inspired minimal style
- 앱 아이콘 그린에서 파생한 다크 그린 포인트 컬러 기반
- Pretendard 폰트 패밀리 사용
- Material3 기반이지만 커스텀 팔레트와 타이포그래피로 분위기를 만듦

색상은 밝은 모드/다크 모드를 모두 지원하며,
밝은 모드는 iOS 느낌의 밝은 회색 배경과 흰 surface,
다크 모드는 거의 검은 배경과 어두운 surface 조합이다.

## 14. 코드 스타일과 관례

이 저장소에서 보이는 스타일은 아래와 같다.

- Kotlin 표준 스타일 기반
- 들여쓰기 4칸
- 클래스/Composable은 PascalCase
- 함수/프로퍼티는 camelCase
- 상수는 UPPER_SNAKE_CASE
- 주석과 문자열은 한국어가 많음
- 에러 처리에 `runCatching` / `Result` 적극 사용
- 비동기 상태는 `Flow`, `StateFlow`, `viewModelScope` 중심
- DI 프레임워크 없이 수동 구성

즉, 복잡한 프레임워크보다 "작게 유지되는 단순한 안드로이드 프로젝트" 쪽에 가까운 코드베이스다.

## 15. 실행/개발/검증 커맨드

현재 저장소에서 바로 유용한 명령은 아래와 같다.

```bash
./gradlew assembleDebug
./gradlew app:build
./gradlew app:lintDebug
./gradlew app:testDebugUnitTest
./gradlew app:check
./gradlew app:installDebug
./gradlew clean
./gradlew tasks --all
```

의미 요약:

- `assembleDebug` - 디버그 APK 빌드
- `app:build` - 앱 빌드 + 테스트 계열 포함 전체 빌드
- `app:lintDebug` - 안드로이드 lint
- `app:testDebugUnitTest` - 로컬 unit test
- `app:check` - 검증 작업 전체
- `app:installDebug` - 연결된 디바이스/에뮬레이터에 설치

## 16. CI / 훅 정보

### 16.1 GitHub Actions

파일:

- `.github/workflows/android-build.yml`

현재 Android CI는 아래 흐름이다.

1. 체크아웃
2. `push to main`이면 `version.properties` 자동 버전 업
3. 변경된 버전 파일을 bot 계정으로 다시 커밋/푸시
4. JDK 17 설정
5. Gradle 8.5 설정
6. `./gradlew assembleDebug assembleRelease bundleRelease` 실행
7. 버전 정보가 포함된 이름으로 디버그 APK 업로드
8. 버전 정보가 포함된 이름으로 release APK 및 release AAB 업로드

즉, `main`에 커밋이 들어가면 버전 충돌을 피하기 위한 자동 버전 업과 debug/release 산출물 빌드가 같이 수행된다.

### 16.2 pre-commit 스크립트

파일:

- `scripts/pre-commit`

이 스크립트는 커밋 전에 `./gradlew assembleDebug`를 실행하도록 되어 있다.
빌드가 실패하면 커밋도 실패하게 설계돼 있다.

따라서 이 프로젝트에서 코드 수정 후 가장 기본적인 검증은 `assembleDebug`다.

## 17. 현재 보이는 리스크 / 주의 포인트

### 17.1 테스트 부재

현재 `app/src/test`나 `app/src/androidTest`에 실질적인 테스트 코드가 보이지 않는다.
그래서 회귀 검증은 사실상 빌드 성공 여부에 많이 의존한다.

### 17.2 destructive migration

`AppDatabase`가 `fallbackToDestructiveMigration()`을 사용한다.
스키마 변경 시 로컬 데이터가 초기화될 수 있다.

이 앱은 캐시 앱 성격이 있어 어느 정도 용인될 수 있지만,
캘린더에서 사용자가 직접 수정한 식단도 DB에 저장되므로 완전히 가벼운 이슈는 아니다.

### 17.3 cleartext traffic 허용

`AndroidManifest.xml`에서 `android:usesCleartextTraffic="true"`가 켜져 있다.
외부 API를 HTTP로 붙이기 쉽게 하려는 의도일 수 있지만,
운영 배포 관점에서는 보안 정책 검토가 필요하다.

### 17.4 외부 API 모드의 자동 갱신 일관성

로컬 모드는 워커 + 증분 동기화 + API key 기반 흐름이 꽤 명확하다.
하지만 외부 API 모드는 워커가 endpoint를 기준으로 주기 갱신하지 않는다.

또한 외부 모드는 timestamp 기반 동기화 판정에 크게 의존하므로,
"18시 이후 내일 식단 표시"와 실제 외부 데이터 준비 시점 사이에서 엣지 케이스가 생길 여지가 있다.

### 17.5 수동 수정 데이터와 원격 동기화의 관계

캘린더 화면에서는 식단을 직접 수정해 DB에 저장할 수 있다.
이후 원격 동기화가 다시 들어오면 메뉴 병합 로직에 의해 예상과 다른 결합 결과가 생길 수 있다.

완전한 "사용자 오버라이드" 정책이 있는 구조는 아니다.

## 18. 다음 작업을 할 때 기억하면 좋은 포인트

이 프로젝트에서 수정 작업을 할 때는 아래 질문을 먼저 떠올리면 좋다.

- 이 변경이 앱 화면만 바꾸는가, 위젯도 같이 바꿔야 하는가?
- 데이터 소스가 local/external 둘 다에서 동작해야 하는가?
- 18시 이후 내일 식단 규칙과 충돌하지 않는가?
- Room 캐시와 DataStore 설정 둘 중 어디를 바꿔야 하는가?
- 위젯 갱신 호출이 필요한 변경인가?
- 외부 모드와 로컬 모드의 동작이 어긋나지 않는가?

특히 이 프로젝트는 앱/위젯/백그라운드 워커/설정 저장소가 느슨하게 연결돼 있으므로,
한 군데만 고치면 끝나는 경우보다 "연동 포인트를 같이 봐야 하는 경우"가 더 많다.

## 19. 작업 후 기본 확인 순서

가장 보수적인 기본 확인 순서는 아래 정도로 잡으면 된다.

1. `./gradlew assembleDebug`
2. 필요 시 `./gradlew app:lintDebug`
3. 필요 시 `./gradlew app:testDebugUnitTest`
4. 위젯 관련 변경이면 실제 위젯 추가/갱신/삭제 흐름 확인
5. 데이터 관련 변경이면 local/external 소스 둘 다 확인

## 20. 한 문장으로 다시 정리

이 저장소는 "국방부 식단 데이터를 Room에 캐시하고, Compose 앱과 Glance 위젯이 그 데이터를 함께 소비하는 Kotlin 안드로이드 프로젝트"이며,
소규모 앱답게 수동 DI와 단순한 계층 분리 위에 실용적으로 구현되어 있다.
