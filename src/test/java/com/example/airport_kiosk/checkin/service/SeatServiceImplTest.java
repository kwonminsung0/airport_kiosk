package com.example.airport_kiosk.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.airport_kiosk.checkin.dto.SeatMapResponse;
import com.example.airport_kiosk.checkin.mapper.SeatMapper;
import com.example.airport_kiosk.checkin.service.impl.SeatServiceImpl;
import com.example.airport_kiosk.checkin.vo.FlightVO;
import com.example.airport_kiosk.checkin.vo.SeatVO;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeatServiceImplTest {

    private final SeatMapper seatMapper = mock(SeatMapper.class);
    private final SeatService seatService = new SeatServiceImpl(seatMapper);

    @Test
    @DisplayName("항공편의 행·열 수와 좌석별 상태를 돌려준다")
    void returnsSeatMap() {
        when(seatMapper.findFlightById(1L)).thenReturn(
                new FlightVO(1L, "ZZ101", "ICN", "NRT", LocalDateTime.of(2026, 11, 27, 17, 0), "12", 10, 6));
        when(seatMapper.findByFlightId(1L)).thenReturn(List.of(
                new SeatVO(11L, 1L, "1A", "AVAILABLE", null),
                new SeatVO(12L, 1L, "1B", "ASSIGNED", "STU111")));

        SeatMapResponse seatMap = seatService.getSeatMap(1L);

        assertThat(seatMap.getFlightId()).isEqualTo(1L);
        assertThat(seatMap.getSeatRows()).isEqualTo(10);
        assertThat(seatMap.getSeatCols()).isEqualTo(6);
        assertThat(seatMap.getSeats())
                .extracting(seat -> seat.getSeatId() + " " + seat.getSeatNo() + " " + seat.getStatus())
                .containsExactly("11 1A AVAILABLE", "12 1B ASSIGNED");
    }

    @Test
    @DisplayName("항공편이 없으면 FLIGHT_NOT_FOUND가 발생하고 좌석은 조회하지 않는다")
    void throwsWhenFlightMissing() {
        when(seatMapper.findFlightById(99L)).thenReturn(null);

        assertThatThrownBy(() -> seatService.getSeatMap(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FLIGHT_NOT_FOUND);
        verify(seatMapper, never()).findByFlightId(99L);
    }
}
