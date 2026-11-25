-- 시연용 데이터. 출발 시각은 앱을 시작한 시각을 기준으로 계산한다.
-- 편명과 이름은 모두 가상이다.

-- 1. 항공편
-- ZZ101: 정상 체크인 (출발 5시간 전)
INSERT INTO FLIGHT (flight_id, flight_no, dep_airport, arr_airport, dep_time, gate, seat_rows, seat_cols)
VALUES (SEQ_FLIGHT.NEXTVAL, 'ZZ101', 'ICN', 'NRT', SYSDATE + INTERVAL '5' HOUR, '12', 5, 4);

-- ZZ102: 체크인 마감 후 (422)
INSERT INTO FLIGHT (flight_id, flight_no, dep_airport, arr_airport, dep_time, gate, seat_rows, seat_cols)
VALUES (SEQ_FLIGHT.NEXTVAL, 'ZZ102', 'ICN', 'KIX', SYSDATE + INTERVAL '30' MINUTE, '15', 5, 4);

-- ZZ103: 체크인 시작 전 (422)
INSERT INTO FLIGHT (flight_id, flight_no, dep_airport, arr_airport, dep_time, gate, seat_rows, seat_cols)
VALUES (SEQ_FLIGHT.NEXTVAL, 'ZZ103', 'ICN', 'FUK', SYSDATE + INTERVAL '30' HOUR, '21', 5, 4);

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

-- 3. 좌석: 항공편마다 1A~5D (5행 x 4열 = 20개), 모두 선택 가능
INSERT INTO SEAT (seat_id, flight_id, seat_no, status)
SELECT SEQ_SEAT.NEXTVAL, f.flight_id, r.row_no || c.col_name, 'AVAILABLE'
FROM FLIGHT f
CROSS JOIN (
    SELECT 1 AS row_no FROM DUAL
    UNION ALL SELECT 2 FROM DUAL
    UNION ALL SELECT 3 FROM DUAL
    UNION ALL SELECT 4 FROM DUAL
    UNION ALL SELECT 5 FROM DUAL
) r
CROSS JOIN (
    SELECT 'A' AS col_name FROM DUAL
    UNION ALL SELECT 'B' FROM DUAL
    UNION ALL SELECT 'C' FROM DUAL
    UNION ALL SELECT 'D' FROM DUAL
) c;
