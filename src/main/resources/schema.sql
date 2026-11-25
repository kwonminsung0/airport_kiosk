-- 앱을 시작할 때마다 테이블을 지우고 다시 만든다. 기존 데이터는 모두 사라진다.

-- 1. 테이블 삭제 (FK 때문에 자식 테이블부터)
DROP TABLE IF EXISTS BAGGAGE;
DROP TABLE IF EXISTS BOARDING_PASS;
DROP TABLE IF EXISTS CHECKIN;
DROP TABLE IF EXISTS SEAT;
DROP TABLE IF EXISTS AL_RESERVATION;
DROP TABLE IF EXISTS FLIGHT;
DROP TABLE IF EXISTS INTERFACE_LOG;

-- 2. 시퀀스 삭제
DROP SEQUENCE IF EXISTS SEQ_FLIGHT;
DROP SEQUENCE IF EXISTS SEQ_SEAT;
DROP SEQUENCE IF EXISTS SEQ_CHECKIN;
DROP SEQUENCE IF EXISTS SEQ_BAGGAGE;
DROP SEQUENCE IF EXISTS SEQ_INTERFACE_LOG;

-- 3. 시퀀스 생성
CREATE SEQUENCE SEQ_FLIGHT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_SEAT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_CHECKIN START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_BAGGAGE START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_INTERFACE_LOG START WITH 1 INCREMENT BY 1;

-- 4. 테이블 생성 (부모 테이블부터)

-- 항공편 (공용)
CREATE TABLE FLIGHT (
    flight_id   NUMBER(19)   NOT NULL,
    flight_no   VARCHAR2(10) NOT NULL,
    dep_airport VARCHAR2(3)  NOT NULL,
    arr_airport VARCHAR2(3)  NOT NULL,
    dep_time    TIMESTAMP    NOT NULL,
    gate        VARCHAR2(5)  NOT NULL,
    seat_rows   NUMBER(3)    NOT NULL,
    seat_cols   NUMBER(3)    NOT NULL,
    CONSTRAINT PK_FLIGHT PRIMARY KEY (flight_id)
);

-- 예약 (가상 항공사)
CREATE TABLE AL_RESERVATION (
    pnr                  VARCHAR2(6)  NOT NULL,
    last_name            VARCHAR2(50) NOT NULL,
    first_name           VARCHAR2(50) NOT NULL,
    flight_id            NUMBER(19)   NOT NULL,
    baggage_allowance_kg NUMBER(3)    NOT NULL,
    CONSTRAINT PK_AL_RESERVATION PRIMARY KEY (pnr),
    CONSTRAINT FK_AL_RESERVATION_FLIGHT FOREIGN KEY (flight_id) REFERENCES FLIGHT (flight_id)
);

-- 좌석 (키오스크). pnr에는 FK를 걸지 않는다 (항공사는 외부 시스템)
CREATE TABLE SEAT (
    seat_id   NUMBER(19)   NOT NULL,
    flight_id NUMBER(19)   NOT NULL,
    seat_no   VARCHAR2(4)  NOT NULL,
    status    VARCHAR2(10) NOT NULL,
    pnr       VARCHAR2(6),
    CONSTRAINT PK_SEAT PRIMARY KEY (seat_id),
    CONSTRAINT FK_SEAT_FLIGHT FOREIGN KEY (flight_id) REFERENCES FLIGHT (flight_id),
    CONSTRAINT UK_SEAT_FLIGHT_SEAT_NO UNIQUE (flight_id, seat_no),
    CONSTRAINT CK_SEAT_STATUS CHECK (status IN ('AVAILABLE', 'ASSIGNED')),
    CONSTRAINT CK_SEAT_STATUS_PNR CHECK (
        (status = 'AVAILABLE' AND pnr IS NULL) OR (status = 'ASSIGNED' AND pnr IS NOT NULL)
    )
);

-- 체크인 (키오스크). pnr에는 FK를 걸지 않는다 (항공사는 외부 시스템)
CREATE TABLE CHECKIN (
    checkin_id    NUMBER(19)   NOT NULL,
    pnr           VARCHAR2(6)  NOT NULL,
    last_name     VARCHAR2(50) NOT NULL,
    first_name    VARCHAR2(50) NOT NULL,
    flight_id     NUMBER(19)   NOT NULL,
    seat_id       NUMBER(19)   NOT NULL,
    checked_in_at TIMESTAMP    NOT NULL,
    CONSTRAINT PK_CHECKIN PRIMARY KEY (checkin_id),
    CONSTRAINT FK_CHECKIN_FLIGHT FOREIGN KEY (flight_id) REFERENCES FLIGHT (flight_id),
    CONSTRAINT FK_CHECKIN_SEAT FOREIGN KEY (seat_id) REFERENCES SEAT (seat_id),
    CONSTRAINT UK_CHECKIN_PNR UNIQUE (pnr),
    CONSTRAINT UK_CHECKIN_SEAT UNIQUE (seat_id)
);

-- 탑승권 (키오스크)
CREATE TABLE BOARDING_PASS (
    boarding_pass_no VARCHAR2(20) NOT NULL,
    checkin_id       NUMBER(19)   NOT NULL,
    gate             VARCHAR2(5)  NOT NULL,
    boarding_time    TIMESTAMP    NOT NULL,
    issued_at        TIMESTAMP    NOT NULL,
    CONSTRAINT PK_BOARDING_PASS PRIMARY KEY (boarding_pass_no),
    CONSTRAINT FK_BOARDING_PASS_CHECKIN FOREIGN KEY (checkin_id) REFERENCES CHECKIN (checkin_id),
    CONSTRAINT UK_BOARDING_PASS_CHECKIN UNIQUE (checkin_id)
);

-- 수하물 (키오스크, 2단계)
CREATE TABLE BAGGAGE (
    baggage_id       NUMBER(19)   NOT NULL,
    boarding_pass_no VARCHAR2(20) NOT NULL,
    weight_kg        NUMBER(5, 2) NOT NULL,
    tag_no           VARCHAR2(10) NOT NULL,
    registered_at    TIMESTAMP    NOT NULL,
    CONSTRAINT PK_BAGGAGE PRIMARY KEY (baggage_id),
    CONSTRAINT FK_BAGGAGE_BOARDING_PASS FOREIGN KEY (boarding_pass_no) REFERENCES BOARDING_PASS (boarding_pass_no),
    CONSTRAINT UK_BAGGAGE_BOARDING_PASS UNIQUE (boarding_pass_no),
    CONSTRAINT UK_BAGGAGE_TAG_NO UNIQUE (tag_no),
    CONSTRAINT CK_BAGGAGE_WEIGHT CHECK (weight_kg > 0)
);

-- 연계 이력 로그 (키오스크)
CREATE TABLE INTERFACE_LOG (
    log_id          NUMBER(19)    NOT NULL,
    interface_name  VARCHAR2(50)  NOT NULL,
    request_summary VARCHAR2(500) NOT NULL,
    result_code     VARCHAR2(50)  NOT NULL,
    success_yn      CHAR(1)       NOT NULL,
    elapsed_ms      NUMBER(10)    NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    CONSTRAINT PK_INTERFACE_LOG PRIMARY KEY (log_id),
    CONSTRAINT CK_INTERFACE_LOG_SUCCESS CHECK (success_yn IN ('Y', 'N'))
);
