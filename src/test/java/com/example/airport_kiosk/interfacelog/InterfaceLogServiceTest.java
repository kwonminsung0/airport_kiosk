package com.example.airport_kiosk.interfacelog;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.airport_kiosk.interfacelog.service.InterfaceLogService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class InterfaceLogServiceTest {

    @Autowired
    private InterfaceLogService interfaceLogService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearLogs() {
        jdbcTemplate.update("DELETE FROM INTERFACE_LOG");
    }

    @Test
    @DisplayName("연계에 성공하면 요청 요약, 결과 코드, 성공 여부 Y, 소요 시간이 저장된다")
    void savesSuccessLog() {
        interfaceLogService.save("AIRLINE_RESERVATION", "pnr=ABC123", "200", true, 35);

        Map<String, Object> log = jdbcTemplate.queryForMap("SELECT * FROM INTERFACE_LOG");
        assertThat(log.get("INTERFACE_NAME")).isEqualTo("AIRLINE_RESERVATION");
        assertThat(log.get("REQUEST_SUMMARY")).isEqualTo("pnr=ABC123");
        assertThat(log.get("RESULT_CODE")).isEqualTo("200");
        assertThat(log.get("SUCCESS_YN")).isEqualTo("Y");
        assertThat(((Number) log.get("ELAPSED_MS")).longValue()).isEqualTo(35);
        assertThat(log.get("CREATED_AT")).isNotNull();
    }

    @Test
    @DisplayName("연계에 실패하면 성공 여부가 N으로 저장된다")
    void savesFailureLog() {
        interfaceLogService.save("AIRLINE_RESERVATION", "pnr=ABC123", "TIMEOUT", false, 3000);

        Map<String, Object> log = jdbcTemplate.queryForMap("SELECT * FROM INTERFACE_LOG");
        assertThat(log.get("RESULT_CODE")).isEqualTo("TIMEOUT");
        assertThat(log.get("SUCCESS_YN")).isEqualTo("N");
    }

    @Test
    @DisplayName("호출한 쪽의 트랜잭션이 롤백되어도 연계 로그는 남는다")
    void keepsLogWhenOuterTransactionRollsBack() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);

        outer.executeWithoutResult(status -> {
            jdbcTemplate.update("UPDATE FLIGHT SET gate = '99' WHERE flight_no = 'ZZ101'");
            interfaceLogService.save("AIRLINE_RESERVATION", "pnr=ABC123", "200", true, 35);
            status.setRollbackOnly();
        });

        String gate = jdbcTemplate.queryForObject("SELECT gate FROM FLIGHT WHERE flight_no = 'ZZ101'", String.class);
        assertThat(gate).as("바깥 트랜잭션의 변경은 롤백된다").isEqualTo("12");
        Integer logs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM INTERFACE_LOG", Integer.class);
        assertThat(logs).as("연계 로그는 남는다").isEqualTo(1);
    }
}
