# 공피하기 게임 (Ball Dodging Game)

Java 21 Swing 기반의 2D 아케이드 공피하기 게임입니다.

## 기능 특징
- **키보드 조작**: 상/하/좌/우(↑, ↓, ←, →) 방향키 및 WASD 지원 (8방향 대각선 이동 가능)
- **점진적 난이도**: 생존 시간에 따라 날아오는 공의 속도와 생성 주기가 점진적으로 빨라짐
- **다양한 공 타입**: 일반 공, 고속 공, 대형 공
- **점수 시스템**: 생존 시간 측정 및 최고 점수(Best Score) 기록
- **부드러운 그래픽**: 60 FPS 기반 게임 루프 및 2D 안티앨리어싱 렌더링

## 실행 요구사항
- Java 21 이상 (JDK 21)

## 실행 방법
- `run.bat` 실행 또는 직접 컴파일 후 실행
```bash
javac -d bin -encoding UTF-8 src/com/dodging/game/*.java src/com/dodging/game/*/*.java
java -cp bin com.dodging.game.Main
```
