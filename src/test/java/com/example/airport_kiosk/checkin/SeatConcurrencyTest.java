package com.example.airport_kiosk.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.airport_kiosk.checkin.service.SeatService;
import com.example.airport_kiosk.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// 스레드마다 트랜잭션이 따로 커밋되어야 하므로 테스트에 @Transactional을 붙이지 않는다. 대신 끝나면 좌석을 되돌린다
@SpringBootTest
@ActiveProfiles("test")
class SeatConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(SeatConcurrencyTest.class);
    private static final int THREADS = 10;
    // 시연 데이터에서 배정된 2B·2C·6E와 다른 테스트가 쓰는 좌석을 피한 빈 좌석
    private static final String SEAT_NO = "8C";

    @Autowired
    private SeatService seatService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long flightId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        flightId = jdbcTemplate.queryForObject("SELECT flight_id FROM FLIGHT WHERE flight_no = 'ZZ101'", Long.class);
        seatId = jdbcTemplate.queryForObject(
                "SELECT seat_id FROM SEAT WHERE flight_id = ? AND seat_no = ?", Long.class, flightId, SEAT_NO);
        resetSeat();
    }

    @AfterEach
    void resetSeat() {
        jdbcTemplate.update("UPDATE SEAT SET status = 'AVAILABLE', pnr = NULL WHERE seat_id = ?", seatId);
    }

    @Test
    @DisplayName("같은 좌석을 동시에 선택하면 한 명만 성공하고 나머지 9명은 SEAT_ALREADY_TAKEN을 받는다")
    void onlyOneWinsSameSeat() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();

        // 승객마다 다른 예약번호(CON001~CON010)로 같은 좌석을 고른다
        for (int i = 1; i <= THREADS; i++) {
            String pnr = String.format("CON%03d", i);
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await(); // 10개 스레드가 모두 준비된 뒤 한꺼번에 출발한다
                try {
                    seatService.assignSeat(flightId, seatId, pnr);
                    return "SUCCESS " + pnr;
                } catch (BusinessException e) {
                    return e.getErrorCode().name();
                }
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> result : results) {
            // BusinessException이 아닌 예외(잠금 시간 초과 등)는 여기서 그대로 드러나 테스트가 실패한다
            outcomes.add(result.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        List<String> winners = outcomes.stream().filter(outcome -> outcome.startsWith("SUCCESS")).toList();
        long taken = outcomes.stream().filter("SEAT_ALREADY_TAKEN"::equals).count();
        Map<String, Object> seat = jdbcTemplate.queryForMap("SELECT status, pnr FROM SEAT WHERE seat_id = ?", seatId);
        log.info("동시 선택 결과: DB={}, 성공={}건 {}, SEAT_ALREADY_TAKEN={}건, 좌석 {}: status={}, pnr={}",
                jdbcTemplate.execute((ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName()),
                winners.size(), winners, taken, SEAT_NO, seat.get("STATUS"), seat.get("PNR"));

        assertThat(winners).hasSize(1);
        assertThat(taken).isEqualTo(THREADS - 1);
        assertThat(seat.get("STATUS")).isEqualTo("ASSIGNED");
        assertThat(winners.get(0)).isEqualTo("SUCCESS " + seat.get("PNR"));
    }
}
