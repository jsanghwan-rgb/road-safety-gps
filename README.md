# 도로안심 GPS — 독립형 안드로이드 시험 프로젝트
기존 Floot 프로젝트를 대체하는 **네이티브 GPS 기능 검증용 최소 앱**입니다. 원래 Floot 지도/공사 API 화면은 아직 이 프로젝트로 이식되지 않았습니다. 카메라·사고·과속 자동 경고는 미구현이며, 도로 안전 안내에 사용하면 안 됩니다.

Android Studio에서 폴더를 열고 Gradle Sync → Build APK(s). Android SDK 35 및 인터넷을 통한 Gradle 플러그인 설치가 필요합니다. 서명되지 않은 debug APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. 휴대전화에서 GPS 위치 권한을 허용하고, 알림을 확인하세요. 장시간 화면 잠금/절전 시의 위치 수신은 실기기 테스트 필요. Android 11+의 백그라운드 위치 권한과 배터리 최적화는 별도 검증이 필요합니다.
