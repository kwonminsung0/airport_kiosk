package com.example.airport_kiosk.checkin.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.airport_kiosk.checkin.vo.FlightVO;
import com.example.airport_kiosk.checkin.vo.SeatVO;
import java.util.List;
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
class SeatMapperTest {

    @Autowired
    private SeatMapper seatMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("항공편 ID로 항공편과 좌석 행·열 수를 조회한다")
    void findsFlight() {
        FlightVO flight = seatMapper.findFlightById(flightId("ZZ101"));

        assertThat(flight.getFlightNo()).isEqualTo("ZZ101");
        assertThat(flight.getSeatRows()).isEqualTo(10);
        assertThat(flight.getSeatCols()).isEqualTo(6);
    }

    @Test
    @DisplayName("없는 항공편 ID면 null을 돌려준다")
    void returnsNullWhenFlightMissing() {
        assertThat(seatMapper.findFlightById(-1L)).isNull();
    }

    @Test
    @DisplayName("좌석은 행 번호 순서(1A~1F, 2A~ ... 10F)로 조회한다")
    void findsSeatsInRowOrder() {
        List<SeatVO> seats = seatMapper.findByFlightId(flightId("ZZ101"));

        assertThat(seats).hasSize(60);
        assertThat(seats).extracting(SeatVO::getSeatNo)
                .startsWith("1A", "1B", "1C", "1D", "1E", "1F", "2A")
                .endsWith("9F", "10A", "10B", "10C", "10D", "10E", "10F");
    }

    @Test
    @DisplayName("좌석마다 상태를 함께 조회한다 (시연 데이터의 2B는 배정됨)")
    void findsSeatStatus() {
        List<SeatVO> seats = seatMapper.findByFlightId(flightId("ZZ101"));

        SeatVO seat2b = seats.stream().filter(seat -> seat.getSeatNo().equals("2B")).findFirst().orElseThrow();
        SeatVO seat1a = seats.stream().filter(seat -> seat.getSeatNo().equals("1A")).findFirst().orElseThrow();
        assertThat(seat2b.getStatus()).isEqualTo("ASSIGNED");
        assertThat(seat2b.getPnr()).isEqualTo("STU111");
        assertThat(seat1a.getStatus()).isEqualTo("AVAILABLE");
        assertThat(seat1a.getPnr()).isNull();
    }

    private Long flightId(String flightNo) {
        return jdbcTemplate.queryForObject("SELECT flight_id FROM FLIGHT WHERE flight_no = ?", Long.class, flightNo);
    }
}
