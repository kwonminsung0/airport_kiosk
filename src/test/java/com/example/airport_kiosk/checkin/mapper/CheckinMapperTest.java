package com.example.airport_kiosk.checkin.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CheckinMapperTest {

    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("예약번호로 체크인 기록의 수를 센다")
    void countsByPnr() {
        jdbcTemplate.update("""
                INSERT INTO CHECKIN (checkin_id, pnr, last_name, first_name, flight_id, seat_id, checked_in_at)
                SELECT SEQ_CHECKIN.NEXTVAL, 'ABC123', 'KIM', 'MINSU', s.flight_id, s.seat_id, SYSTIMESTAMP
                FROM SEAT s WHERE s.seat_id = (SELECT MIN(seat_id) FROM SEAT)
                """);

        assertThat(checkinMapper.countByPnr("ABC123")).isEqualTo(1);
        assertThat(checkinMapper.countByPnr("DEF456")).isZero();
    }
}
