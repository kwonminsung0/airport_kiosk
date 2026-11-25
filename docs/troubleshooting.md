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

## [2026-11-25] 항공사 연계가 실패해도 서버 로그에 원인이 남지 않음

- 문제: 항공사 연결 실패로 502가 나도 서버 로그에 원인 예외가 없었다. 연계 로그(`INTERFACE_LOG`)에는 결과 코드만 남아 원인을 알 수 없었다.
- 원인: 커밋 전 코드를 확인하다가 "원인 예외가 실제로 로그에 남는가?"를 점검했다. `AirlineClient`는 원인을 `BusinessException`의 cause에 담기만 했고, `GlobalExceptionHandler`도 `BusinessException`은 로그로 남기지 않았다. 항공사 주소를 닫힌 포트로 바꿔 실행해 로그가 없는 것을 확인했다.
- 해결: 실패하는 테스트를 먼저 작성한 뒤(6건 중 5건 실패), `AirlineClient`의 실패 처리에 `log.warn`을 추가했다. 통신 실패는 원인 예외까지, 응답을 받은 뒤의 오류는 예외 종류만 남긴다. (커밋 `49d8d9b`)
- 배운 점: 예외를 다른 예외로 바꿀 때는 원인을 어디에서 기록하는지 함께 정해야 한다. 로그에는 원인을 남기되 개인정보가 들어갈 수 있는 내용은 빼야 한다.
