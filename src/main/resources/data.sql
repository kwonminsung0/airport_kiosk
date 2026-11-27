-- 시연용 데이터. 출발 시각은 앱을 시작한 시각을 기준으로 계산한다.
-- 실제 항공편 출발 시각은 분 단위라서 초를 버린다. TRUNC(날짜, 'MI')는 H2(MODE=Oracle)에서 실행 오류가 나므로,
-- H2와 Oracle 모두에서 동작하는 TO_DATE(TO_CHAR(..., 'YYYYMMDDHH24MI'), 'YYYYMMDDHH24MI')를 쓴다.
-- 편명과 이름은 모두 가상이다.

-- 1. 항공편
-- ZZ101: 정상 체크인 (출발 5시간 전)
INSERT INTO FLIGHT (flight_id, flight_no, dep_airport, arr_airport, dep_time, gate, seat_rows, seat_cols)
VALUES (SEQ_FLIGHT.NEXTVAL, 'ZZ101', 'ICN', 'NRT', TO_DATE(TO_CHAR(SYSDATE + INTERVAL '5' HOUR, 'YYYYMMDDHH24MI'), 'YYYYMMDDHH24MI'), '12', 10, 6);

-- ZZ102: 체크인 마감 후 (422)
INSERT INTO FLIGHT (flight_id, flight_no, dep_airport, arr_airport, dep_time, gate, seat_rows, seat_cols)
VALUES (SEQ_FLIGHT.NEXTVAL, 'ZZ102', 'ICN', 'KIX', TO_DATE(TO_CHAR(SYSDATE + INTERVAL '30' MINUTE, 'YYYYMMDDHH24MI'), 'YYYYMMDDHH24MI'), '15', 10, 6);

-- ZZ103: 체크인 시작 전 (422)
INSERT INTO FLIGHT (flight_id, flight_no, dep_airport, arr_airport, dep_time, gate, seat_rows, seat_cols)
VALUES (SEQ_FLIGHT.NEXTVAL, 'ZZ103', 'ICN', 'FUK', TO_DATE(TO_CHAR(SYSDATE + INTERVAL '30' HOUR, 'YYYYMMDDHH24MI'), 'YYYYMMDDHH24MI'), '21', 10, 6);

-- 2. 예약
-- ABC123: 정상
INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('ABC123', 'KIM', 'MINSU', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101'), 23);

-- DEF456: 좌석 중복 화면 확인
INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('DEF456', 'LEE', 'JIYOUNG', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101'), 23);

-- GHI789: 허용량이 다른 예약
INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('GHI789', 'PARK', 'JUNHO', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101'), 15);

-- JKL012: 체크인 마감 후 (422)
INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('JKL012', 'CHOI', 'SUJIN', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ102'), 23);

-- MNO345: 체크인 시작 전 (422)
INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('MNO345', 'JUNG', 'HANEUL', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ103'), 23);

-- STU111, VWX222, YZA333: 이미 체크인한 승객 (ZZ101 2B·2C·6E 배정됨, 조회하면 409)
INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('STU111', 'YOON', 'SEOYEON', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101'), 23);

INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('VWX222', 'KANG', 'DOYUN', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101'), 23);

INSERT INTO AL_RESERVATION (pnr, last_name, first_name, flight_id, baggage_allowance_kg)
VALUES ('YZA333', 'HAN', 'JIWOO', (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101'), 23);

-- 3. 좌석: 단거리 항공기처럼 3-3 배치(A B C | D E F). 항공편마다 1A~10F (10행 x 6열 = 60개)
-- 항공편의 seat_rows, seat_cols까지만 만들어 SEAT가 항상 FLIGHT의 행·열 수와 맞게 한다. 통로 위치는 화면이 열 수로 계산한다
INSERT INTO SEAT (seat_id, flight_id, seat_no, status)
SELECT SEQ_SEAT.NEXTVAL, f.flight_id, r.row_no || c.col_name, 'AVAILABLE'
FROM FLIGHT f
CROSS JOIN (
    SELECT 1 AS row_no FROM DUAL
    UNION ALL SELECT 2 FROM DUAL
    UNION ALL SELECT 3 FROM DUAL
    UNION ALL SELECT 4 FROM DUAL
    UNION ALL SELECT 5 FROM DUAL
    UNION ALL SELECT 6 FROM DUAL
    UNION ALL SELECT 7 FROM DUAL
    UNION ALL SELECT 8 FROM DUAL
    UNION ALL SELECT 9 FROM DUAL
    UNION ALL SELECT 10 FROM DUAL
) r
CROSS JOIN (
    SELECT 1 AS col_no, 'A' AS col_name FROM DUAL
    UNION ALL SELECT 2, 'B' FROM DUAL
    UNION ALL SELECT 3, 'C' FROM DUAL
    UNION ALL SELECT 4, 'D' FROM DUAL
    UNION ALL SELECT 5, 'E' FROM DUAL
    UNION ALL SELECT 6, 'F' FROM DUAL
) c
WHERE r.row_no <= f.seat_rows
  AND c.col_no <= f.seat_cols;

-- 4. 이미 체크인한 승객의 좌석과 체크인 기록
-- CK_SEAT_STATUS_PNR 때문에 status와 pnr을 함께 바꾼다
UPDATE SEAT SET status = 'ASSIGNED', pnr = 'STU111'
WHERE flight_id = (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101') AND seat_no = '2B';

UPDATE SEAT SET status = 'ASSIGNED', pnr = 'VWX222'
WHERE flight_id = (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101') AND seat_no = '2C';

UPDATE SEAT SET status = 'ASSIGNED', pnr = 'YZA333'
WHERE flight_id = (SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101') AND seat_no = '6E';

-- 탑승권(BOARDING_PASS)은 4단계에서 탑승권 번호 형식을 정한 뒤 함께 넣는다
INSERT INTO CHECKIN (checkin_id, pnr, last_name, first_name, flight_id, seat_id, checked_in_at)
SELECT SEQ_CHECKIN.NEXTVAL, r.pnr, r.last_name, r.first_name, r.flight_id, s.seat_id, SYSDATE - INTERVAL '1' HOUR
FROM AL_RESERVATION r
JOIN SEAT s ON s.flight_id = r.flight_id AND s.pnr = r.pnr
WHERE r.pnr IN ('STU111', 'VWX222', 'YZA333');
