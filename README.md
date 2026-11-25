# 셀프체크인·셀프백드랍 미니 키오스크 시스템

공항 키오스크의 셀프체크인과 셀프백드랍 흐름을 작은 규모로 구현하는 개인 프로젝트입니다.

공항 셀프체크인·셀프백드랍 플랫폼과 항공사 시스템 연계 업무를 직접 만들어 보며 이해하기 위해 시작했습니다.

> 개발 중인 프로젝트입니다. "작성 예정" 항목은 기능을 구현하면서 채웁니다.

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 4.0.10 |
| DB 접근 | MyBatis Spring Boot Starter 4.0.1 (XML 매퍼) |
| DB | Oracle AI Database 26ai Free (23.26.4, 컨테이너) |
| 테스트 DB | H2 (`MODE=Oracle`) |
| 빌드 | Gradle - Groovy |
| 화면 | HTML, CSS, Vanilla JS |
| 테스트 | JUnit 5, Mockito, Spring Boot Test |

## 개발 방식

Claude Code를 보조 도구로 사용했습니다. 요구사항 정의, 설계 결정, 코드 검토, 테스트 확인은 직접 하고, 모든 코드는 직접 설명할 수 있는 범위로만 반영합니다.

## 실행 방법

작성 예정

## 주요 기능과 흐름

작성 예정

## ERD

```mermaid
erDiagram
    FLIGHT ||--o{ AL_RESERVATION : "예약됨"
    FLIGHT ||--o{ SEAT : "보유"
    FLIGHT ||--o{ CHECKIN : "대상 항공편"
    AL_RESERVATION ||..o| CHECKIN : "체크인 (논리 관계)"
    AL_RESERVATION |o..o| SEAT : "좌석 배정 (논리 관계)"
    SEAT ||--o| CHECKIN : "배정"
    CHECKIN ||--|| BOARDING_PASS : "발급"
    BOARDING_PASS ||--o| BAGGAGE : "위탁 (2단계)"

    FLIGHT {
        NUMBER(19) flight_id PK
        VARCHAR2(10) flight_no "편명"
        VARCHAR2(3) dep_airport "출발 공항"
        VARCHAR2(3) arr_airport "도착 공항"
        TIMESTAMP dep_time "출발 시각"
        VARCHAR2(5) gate "탑승구"
        NUMBER(3) seat_rows "좌석 행 수"
        NUMBER(3) seat_cols "좌석 열 수"
    }
    AL_RESERVATION {
        VARCHAR2(6) pnr PK "예약번호"
        VARCHAR2(50) last_name "영문 성"
        VARCHAR2(50) first_name "영문 이름"
        NUMBER(19) flight_id FK
        NUMBER(3) baggage_allowance_kg "무료 수하물 허용량"
    }
    SEAT {
        NUMBER(19) seat_id PK
        NUMBER(19) flight_id FK "UNIQUE(flight_id, seat_no)"
        VARCHAR2(4) seat_no "좌석 번호, 예: 12A"
        VARCHAR2(10) status "CHECK: AVAILABLE 또는 ASSIGNED"
        VARCHAR2(6) pnr "CHECK: AVAILABLE이면 NULL, ASSIGNED이면 필수"
    }
    CHECKIN {
        NUMBER(19) checkin_id PK
        VARCHAR2(6) pnr UK "중복 체크인 방지"
        VARCHAR2(50) last_name "체크인 시점의 성"
        VARCHAR2(50) first_name "체크인 시점의 이름"
        NUMBER(19) flight_id FK
        NUMBER(19) seat_id FK, UK "좌석 중복 배정 방지"
        TIMESTAMP checked_in_at "체크인 시각"
    }
    BOARDING_PASS {
        VARCHAR2(20) boarding_pass_no PK "탑승권 번호"
        NUMBER(19) checkin_id FK, UK
        VARCHAR2(5) gate "발급 시점의 탑승구"
        TIMESTAMP boarding_time "탑승 시각"
        TIMESTAMP issued_at "발급 시각"
    }
    BAGGAGE {
        NUMBER(19) baggage_id PK
        VARCHAR2(20) boarding_pass_no FK, UK "탑승권당 1개"
        NUMBER weight_kg "무게, NUMBER(5,2), CHECK 0 초과"
        VARCHAR2(10) tag_no UK "태그 번호, 숫자 10자리"
        TIMESTAMP registered_at "등록 시각"
    }
    INTERFACE_LOG {
        NUMBER(19) log_id PK
        VARCHAR2(50) interface_name "연계명"
        VARCHAR2(500) request_summary "요청 요약"
        VARCHAR2(50) result_code "결과 코드"
        CHAR(1) success_yn "CHECK: Y 또는 N"
        NUMBER(10) elapsed_ms "소요 시간"
        TIMESTAMP created_at "기록 시각"
    }
```

- 실선은 DB의 외래 키(FK), 점선은 FK 없이 값으로만 연결되는 논리 관계입니다.
- `AL_RESERVATION`은 가상 항공사, `FLIGHT`는 공용, 나머지는 키오스크의 테이블입니다.
- `INTERFACE_LOG`는 다른 테이블과 관계없이 연계 기록만 쌓습니다.
- `NUMBER` 타입 PK는 Oracle SEQUENCE로 생성합니다.

### 설계 결정

| 결정 | 이유 |
|---|---|
| `CHECKIN`에 `last_name`, `first_name`을 저장 | 탑승권에 이름이 필요합니다. 항공사 테이블을 직접 읽지 않도록 체크인 시점의 이름을 저장합니다. |
| 키오스크 테이블의 `pnr`에는 `AL_RESERVATION` FK를 걸지 않음 | 항공사는 외부 시스템 역할이므로 FK 대신 항공사 API 조회로 예약을 확인합니다. |
| `CHECKIN`에 `UNIQUE(seat_id)` 추가 | 좌석 중복 배정은 조건부 UPDATE로 1차, DB 제약으로 2차 방어합니다. |
| `BAGGAGE`에 `UNIQUE(boarding_pass_no)` 추가 | 수하물은 탑승권당 1개라는 범위를 DB 제약으로도 지킵니다. |
| H2는 테스트에서만 사용 | Oracle 설정이 빠졌을 때 H2로 실행되지 않고 오류가 바로 드러나게 합니다. |

## API 목록

작성 예정

## 테스트

작성 예정

## 트러블슈팅

| 날짜 | 문제 | 해결 |
|---|---|---|
| 2026-11-24 | JAVA_HOME이 잡히지 않아 Gradle JVM 오류 | JDK 홈 경로로 `JAVA_HOME`을 다시 지정 |

자세한 내용은 [docs/troubleshooting.md](docs/troubleshooting.md)에 있습니다.

## 단순화한 부분

- 항공사 시스템은 같은 애플리케이션 안의 가상 항공사 API로 대신합니다. 키오스크는 이 API를 HTTP로 호출합니다.
- 바코드 스캐너와 저울은 화면 입력으로, 프린터는 화면 표시로 대신합니다.
- 예약 1건은 승객 1명, 편도 항공편 1구간입니다.
- 실제 IATA 규격을 따르지 않습니다.
