# 도로안심 GPS — 지도/GPS 안드로이드 시험판

## 현재 구현
- OpenStreetMap 지도(인터넷 연결 필요, Leaflet CDN 이용)
- Android 위치 서비스 GPS 수신, 현재 위치 마커, 정확도 원, 실시간 속도 표시
- 포그라운드 서비스로 화면 꺼짐 중 GPS 수신 시도, 상단 알림에 속도 표시
- 사용자 지정 **절대 속도** 기준(30~150km/h) 초과 시 30초 간격 진동 또는 음성 경고
- 시작/중지, 현재 위치로 지도 이동, 음성 켜기/끄기

## 구현되지 않은 기능 및 안전 주의
- **실제 제한속도, 과속단속 카메라, 사고, 공사 정보 API는 연결되지 않았습니다.** 사용자 지정 속도 기준은 법정 제한속도가 아닙니다.
- 이 앱은 운전 안전 안내용 완성품이 아닌 개발 시험판입니다. 운전 중 앱을 조작하지 마세요.
- Android 11+ 장시간 백그라운드 GPS, 배터리 최적화, 음악과 음성 안내의 공존, 실제 도로 주행은 기기 테스트가 필요합니다.
- 현재 지도는 OSM 타일과 외부 CDN에 의존합니다. 오프라인 지도는 지원하지 않습니다.

## APK 생성
GitHub Actions 탭의 `Build Android APK` 성공 실행을 열고 하단 Artifacts의 `RoadSafetyGPS-APK`를 다운로드합니다. 또는 Android Studio로 프로젝트 폴더를 열고 Gradle Sync 후 Build APK(s). Android SDK 35 및 Java 17이 필요합니다.

## 업데이트
GitHub 저장소 루트에 이 ZIP의 **폴더 안 파일/폴더**를 업로드해야 합니다. `app/`, `.github/`, `build.gradle`, `settings.gradle`, `gradle.properties`, `README.md`를 저장소 루트에 덮어씁니다. ZIP 파일 하나만 올려서는 빌드가 갱신되지 않습니다.
