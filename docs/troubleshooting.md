# 트러블슈팅

## [2026-11-24] JAVA_HOME이 잡히지 않아 Gradle JVM 오류

- 문제: IntelliJ에서 "잘못된 Gradle JVM 구성을 발견했습니다. JAVA_HOME 환경 변수를 수정하세요." 알림이 나왔다.
- 원인: `~/.zshrc`의 `JAVA_HOME`이 JDK 홈(`.../Contents/Home`)이 아닌 Homebrew 폴더(`/opt/homebrew/opt/openjdk@17`)를 가리키고 있었던 것으로 보인다. Homebrew OpenJDK는 macOS 기본 Java 폴더에 등록되지 않아 `/usr/libexec/java_home -V`로도 찾을 수 없었다(`Unable to locate a Java Runtime`).
- 해결: `JAVA_HOME`을 `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`으로 지정하고 `~/.zshrc`의 중복·잘못된 줄을 정리했다. IntelliJ의 Gradle JVM을 `JAVA_HOME`으로 선택한 뒤 `./gradlew test`가 성공했다. (개인 환경 설정이라 커밋 없음)
- 배운 점: 환경 변수 파일은 아래 줄이 위를 덮어쓰므로, 설정을 추가할 때 기존 줄이 있는지 먼저 확인해야 한다.

## [2026-11-25] data.sql 추가 후 테스트에서 시퀀스를 찾지 못함

- 문제: `data.sql`을 추가하자 `contextLoads()`가 `Column "SEQ_FLIGHT.NEXTVAL" not found` 오류로 실패했다.
- 원인: DB 주소 설정이 없어 Spring Boot가 기본 H2를 사용했고, 내장 DB라서 SQL 파일을 자동 실행했다. 기본 H2는 Oracle 모드가 아니어서 `NEXTVAL`과 `SYSDATE`가 통하지 않았다. 테스트 결과 XML의 DB 주소를 확인하고, H2를 두 모드로 실행해 비교했다.
- 해결: `src/test/resources/application-test.yml`에 `MODE=Oracle` H2 주소를 두고 테스트에 `@ActiveProfiles("test")`를 붙였다. 실행용 DB는 `application.yml`에 Oracle로 지정했다. (커밋 `1d3c71e`)
- 배운 점: Spring Boot는 DB 주소가 없으면 내장 DB를 자동 설정하고 `schema.sql`·`data.sql`도 실행한다. 어떤 DB에 연결됐는지 먼저 확인해야 한다.
