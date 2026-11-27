package com.example.airport_kiosk;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseInitTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("테스트는 Oracle 모드의 H2에 연결된다")
    void connectsToH2InOracleMode() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
        String mode = jdbcTemplate.queryForObject(
                "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME = 'MODE'", String.class);
        assertThat(mode).isEqualTo("Oracle");
    }

    @Test
    @DisplayName("schema.sql과 data.sql이 실행되어 항공편 3건, 예약 8건, 좌석 180개, 체크인 3건이 들어간다")
    void loadsInitialData() {
        assertThat(count("FLIGHT")).isEqualTo(3);
        assertThat(count("AL_RESERVATION")).isEqualTo(8);
        assertThat(count("SEAT")).isEqualTo(180);
        assertThat(count("CHECKIN")).isEqualTo(3);
    }

    @Test
    @DisplayName("항공편마다 좌석이 10행 x 6열(1A~10F) 60개이고, 좌석 수가 항공편의 행 x 열과 같다")
    void createsSeatsFromFlightRowsAndCols() {
        List<Map<String, Object>> flights = jdbcTemplate.queryForList("""
                SELECT f.flight_no, f.seat_rows, f.seat_cols, COUNT(s.seat_id) AS seat_count,
                       MIN(s.seat_no) AS min_seat_no, MAX(s.seat_no) AS max_seat_no
                FROM FLIGHT f LEFT JOIN SEAT s ON s.flight_id = f.flight_id
                GROUP BY f.flight_no, f.seat_rows, f.seat_cols
                """);

        assertThat(flights).hasSize(3).allSatisfy(flight -> {
            assertThat(((Number) flight.get("SEAT_ROWS")).intValue()).isEqualTo(10);
            assertThat(((Number) flight.get("SEAT_COLS")).intValue()).isEqualTo(6);
            assertThat(((Number) flight.get("SEAT_COUNT")).intValue()).isEqualTo(60);
            // 문자열 순서라 MIN은 10A, MAX는 9F
            assertThat(flight.get("MIN_SEAT_NO")).isEqualTo("10A");
            assertThat(flight.get("MAX_SEAT_NO")).isEqualTo("9F");
        });
    }

    @Test
    @DisplayName("시연용 체크인 승객 3명은 좌석이 배정되어 있고, 체크인 기록의 좌석과 같다")
    void seedsCheckedInPassengers() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT c.pnr, s.seat_no, s.status, s.pnr AS seat_pnr, f.flight_no
                FROM CHECKIN c
                JOIN SEAT s ON s.seat_id = c.seat_id
                JOIN FLIGHT f ON f.flight_id = c.flight_id AND f.flight_id = s.flight_id
                JOIN AL_RESERVATION r ON r.pnr = c.pnr AND r.flight_id = c.flight_id
                ORDER BY c.pnr
                """);

        assertThat(rows).extracting(row -> row.get("PNR") + " " + row.get("SEAT_NO") + " " + row.get("STATUS")
                        + " " + row.get("SEAT_PNR") + " " + row.get("FLIGHT_NO"))
                .containsExactly(
                        "STU111 2B ASSIGNED STU111 ZZ101",
                        "VWX222 2C ASSIGNED VWX222 ZZ101",
                        "YZA333 6E ASSIGNED YZA333 ZZ101");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEAT WHERE status = 'ASSIGNED'", Integer.class))
                .isEqualTo(3);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
