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
- 2차 리뷰 추가 발견 (2026-11-26)
  - 문제: 개인정보를 빼고 로그를 남기도록 고쳤지만, `Accept`가 맞지 않는 요청에서는 다른 경로로 항공사 응답 본문이 로그에 남고 있었다.
  - 원인: 오류 응답의 형식을 정하지 않아 JSON을 받지 않는 요청에서 응답 쓰기가 실패했다. 처리되지 않은 예외를 서버가 원인까지 통째로 기록했다.
  - 해결: 실패하는 테스트 7건을 먼저 작성하고 오류 응답을 항상 JSON으로 고정했다. 같은 조건에서 로그의 응답 본문이 3건에서 0건이 됐다. (커밋 `2a8da47`) 이어서 응답을 받은 뒤의 오류는 예외에 원인을 담지 않도록 바꿔, 응답 쓰기가 실패해도 본문이 남지 않게 했다. (커밋 `b621c9c`)
  - 배운 점: 직접 쓰는 로그만 막아서는 부족하다. 처리되지 않은 예외가 기록되는 경로와 오류 경로도 시험하고, 예외에 무엇을 담는지도 정해야 한다.
