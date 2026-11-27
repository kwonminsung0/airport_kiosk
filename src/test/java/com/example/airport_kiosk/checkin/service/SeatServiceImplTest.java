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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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

    @Test
    @DisplayName("빈 좌석이면 배정에 성공하고 좌석을 다시 조회하지 않는다")
    void assignsSeat() {
        when(seatMapper.assignSeat(1L, 11L, "ABC123")).thenReturn(1);

        seatService.assignSeat(1L, 11L, "ABC123");

        verify(seatMapper).assignSeat(1L, 11L, "ABC123");
        verify(seatMapper, never()).findSeatById(11L);
    }

    @Test
    @DisplayName("0행이 바뀌었고 이 항공편의 좌석이면 SEAT_ALREADY_TAKEN이 발생한다")
    void throwsWhenSeatTaken() {
        when(seatMapper.assignSeat(1L, 12L, "ABC123")).thenReturn(0);
        when(seatMapper.findSeatById(12L)).thenReturn(new SeatVO(12L, 1L, "1B", "ASSIGNED", "STU111"));

        assertErrorCode(() -> seatService.assignSeat(1L, 12L, "ABC123"), ErrorCode.SEAT_ALREADY_TAKEN);
    }

    @Test
    @DisplayName("0행이 바뀌었고 좌석이 없으면 SEAT_NOT_FOUND가 발생한다")
    void throwsWhenSeatMissing() {
        when(seatMapper.assignSeat(1L, 99L, "ABC123")).thenReturn(0);
        when(seatMapper.findSeatById(99L)).thenReturn(null);

        assertErrorCode(() -> seatService.assignSeat(1L, 99L, "ABC123"), ErrorCode.SEAT_NOT_FOUND);
    }

    @Test
    @DisplayName("0행이 바뀌었고 다른 항공편의 좌석이면 SEAT_NOT_FOUND가 발생한다")
    void throwsWhenSeatOfOtherFlight() {
        when(seatMapper.assignSeat(1L, 70L, "ABC123")).thenReturn(0);
        when(seatMapper.findSeatById(70L)).thenReturn(new SeatVO(70L, 2L, "5A", "AVAILABLE", null));

        assertErrorCode(() -> seatService.assignSeat(1L, 70L, "ABC123"), ErrorCode.SEAT_NOT_FOUND);
    }

    private void assertErrorCode(ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(errorCode);
    }
}
