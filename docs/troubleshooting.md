# 트러블슈팅

## [2026-11-24] JAVA_HOME이 잡히지 않아 Gradle JVM 오류

- 문제: IntelliJ에서 "잘못된 Gradle JVM 구성을 발견했습니다. JAVA_HOME 환경 변수를 수정하세요." 알림이 나왔다.
- 원인: `~/.zshrc`의 `JAVA_HOME`이 JDK 홈(`.../Contents/Home`)이 아닌 Homebrew 폴더(`/opt/homebrew/opt/openjdk@17`)를 가리키고 있었던 것으로 보인다. Homebrew OpenJDK는 macOS 기본 Java 폴더에 등록되지 않아 `/usr/libexec/java_home -V`로도 찾을 수 없었다(`Unable to locate a Java Runtime`).
- 해결: `JAVA_HOME`을 `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`으로 지정하고 `~/.zshrc`의 중복·잘못된 줄을 정리했다. IntelliJ의 Gradle JVM을 `JAVA_HOME`으로 선택한 뒤 `./gradlew test`가 성공했다. (개인 환경 설정이라 커밋 없음)
- 배운 점: 환경 변수 파일은 아래 줄이 위를 덮어쓰므로, 설정을 추가할 때 기존 줄이 있는지 먼저 확인해야 한다.
