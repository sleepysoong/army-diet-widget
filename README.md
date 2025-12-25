# Army Diet Widget (짬수첩)

국방부 공공데이터 포털 API를 활용한 안드로이드 식단 위젯 앱입니다.

## Features
- 오늘/내일 식단 조회 (18시 기준 자동 전환)
- API Key 관리 (DataStore)
- 오프라인 지원 (Room Database)
- 매일 자동 동기화 (WorkManager)

## Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/sleepysoong/army-diet-widget.git
   ```

2. **Setup Git Hooks** (Optional but recommended)
   커밋 전 자동으로 빌드를 검증하려면 아래 스크립트를 실행하세요.
   ```bash
   ./setup_hooks.sh
   ```

3. **API Key**
   앱 실행 후 API Key를 입력해야 합니다.

## Tech Stack
- Kotlin, Jetpack Compose
- MVVM, Clean Architecture
- Retrofit, Room, WorkManager, DataStore
