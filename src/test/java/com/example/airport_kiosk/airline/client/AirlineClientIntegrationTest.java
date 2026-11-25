package com.example.airport_kiosk.airline.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.airport_kiosk.common.config.RestClientConfig;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import com.example.airport_kiosk.interfacelog.service.InterfaceLogService;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AirlineClientIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private InterfaceLogService interfaceLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AirlineClient airlineClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM INTERFACE_LOG");
        // 테스트 서버의 포트는 실행할 때 정해지므로 주소를 직접 넣어 만든다
        RestClient restClient = new RestClientConfig().airlineRestClient(
                "http://localhost:" + port + "/mock-airline", Duration.ofSeconds(1), Duration.ofSeconds(3));
        airlineClient = new AirlineClient(restClient, interfaceLogService);
    }

    @Test
    @DisplayName("실제 HTTP로 가상 항공사를 호출해 예약을 받고 성공 로그를 저장한다")
    void callsMockAirline() {
        AirlineReservation reservation = airlineClient.findReservation("ABC123", "KIM");

        assertThat(reservation.getFirstName()).isEqualTo("MINSU");
        assertThat(reservation.getFlightNo()).isEqualTo("ZZ101");
        assertThat(reservation.getDepTime()).isNotNull();
        assertLog("200");
    }

    @Test
    @DisplayName("없는 예약이면 RESERVATION_NOT_FOUND가 발생하고 404를 성공 로그로 저장한다")
    void throwsNotFound() {
        assertThatThrownBy(() -> airlineClient.findReservation("ZZZ999", "KIM"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        assertLog("404");
    }

    private void assertLog(String resultCode) {
        Map<String, Object> log = jdbcTemplate.queryForMap("SELECT * FROM INTERFACE_LOG");
        assertThat(log.get("RESULT_CODE")).isEqualTo(resultCode);
        assertThat(log.get("SUCCESS_YN")).isEqualTo("Y");
        assertThat(((Number) log.get("ELAPSED_MS")).longValue()).isGreaterThanOrEqualTo(0);
    }
}
