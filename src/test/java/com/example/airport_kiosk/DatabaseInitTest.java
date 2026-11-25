package com.example.airport_kiosk;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
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
    @DisplayName("schema.sql과 data.sql이 실행되어 항공편 3건, 예약 5건, 좌석 60개가 들어간다")
    void loadsInitialData() {
        assertThat(count("FLIGHT")).isEqualTo(3);
        assertThat(count("AL_RESERVATION")).isEqualTo(5);
        assertThat(count("SEAT")).isEqualTo(60);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
