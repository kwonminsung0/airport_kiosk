package com.example.airport_kiosk.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.airport_kiosk.airline.client.AirlineClient;
import com.example.airport_kiosk.airline.client.AirlineReservation;
import com.example.airport_kiosk.checkin.dto.ReservationLookupResponse;
import com.example.airport_kiosk.checkin.mapper.CheckinMapper;
import com.example.airport_kiosk.checkin.service.impl.CheckinServiceImpl;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CheckinServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 12, 0, 0);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final AirlineClient airlineClient = mock(AirlineClient.class);
    private final CheckinMapper checkinMapper = mock(CheckinMapper.class);
    private final CheckinService checkinService = createService(Duration.ofHours(24), Duration.ofHours(1));

    @Test
    @DisplayName("체크인할 수 있는 예약이면 예약과 항공편 정보를 돌려준다")
    void returnsReservation() {
        givenDepartureAt(NOW.plusHours(5));

        ReservationLookupResponse response = checkinService.lookupReservation("ABC123", "KIM");

        assertThat(response.getPnr()).isEqualTo("ABC123");
        assertThat(response.getLastName()).isEqualTo("KIM");
        assertThat(response.getFirstName()).isEqualTo("MINSU");
        assertThat(response.getFlightId()).isEqualTo(1L);
        assertThat(response.getFlightNo()).isEqualTo("ZZ101");
        assertThat(response.getDepAirport()).isEqualTo("ICN");
        assertThat(response.getArrAirport()).isEqualTo("NRT");
        assertThat(response.getDepTime()).isEqualTo(NOW.plusHours(5));
        assertThat(response.getGate()).isEqualTo("12");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("항공사 조회가 실패하면 그 오류를 전달하고 체크인 기록은 조회하지 않는다")
    @EnumSource(value = ErrorCode.class, names = {"RESERVATION_NOT_FOUND", "AIRLINE_ERROR", "AIRLINE_TIMEOUT"})
    void propagatesAirlineError(ErrorCode errorCode) {
        when(airlineClient.findReservation("ABC123", "KIM")).thenThrow(new BusinessException(errorCode));

        assertErrorCode(errorCode);
        verifyNoInteractions(checkinMapper);
    }

    @Test
    @DisplayName("이미 체크인한 예약이면 ALREADY_CHECKED_IN이 발생한다")
    void throwsAlreadyCheckedIn() {
        givenDepartureAt(NOW.plusHours(5));
        when(checkinMapper.countByPnr("ABC123")).thenReturn(1);

        assertErrorCode(ErrorCode.ALREADY_CHECKED_IN);
    }

    @Test
    @DisplayName("체크인이 마감된 뒤에도 이미 체크인한 예약이면 ALREADY_CHECKED_IN이 발생한다")
    void throwsAlreadyCheckedInEvenAfterClosed() {
        givenDepartureAt(NOW.plusMinutes(30));
        when(checkinMapper.countByPnr("ABC123")).thenReturn(1);

        assertErrorCode(ErrorCode.ALREADY_CHECKED_IN);
    }

    @ParameterizedTest(name = "출발 {0}초 전")
    @DisplayName("출발 24시간 전 정각부터 1시간 전 정각까지는 체크인할 수 있다")
    @ValueSource(longs = {24 * 3600, 24 * 3600 - 1, 3600 + 1, 3600})
    void allowsWithinCheckinTime(long secondsBeforeDeparture) {
        givenDepartureAt(NOW.plusSeconds(secondsBeforeDeparture));

        assertThat(checkinService.lookupReservation("ABC123", "KIM").getPnr()).isEqualTo("ABC123");
    }

    @ParameterizedTest(name = "출발 {0}초 전")
    @DisplayName("체크인 시간을 1초라도 벗어나면 CHECKIN_NOT_AVAILABLE이 발생한다")
    @ValueSource(longs = {24 * 3600 + 1, 3600 - 1, 0, -3600})
    void rejectsOutsideCheckinTime(long secondsBeforeDeparture) {
        givenDepartureAt(NOW.plusSeconds(secondsBeforeDeparture));

        assertErrorCode(ErrorCode.CHECKIN_NOT_AVAILABLE);
    }

    // 출발 시각을 "지금 + 시작 기준 + 2시간"으로 두면 시작 시각은 항상 오늘 14:00이다
    @ParameterizedTest(name = "{0} 전 ~ {1} 전 → {2}")
    @DisplayName("체크인 시작 전이면 시작 시각과 설정한 시간으로 안내한다")
    @CsvSource(delimiter = '|', value = {
            "PT24H | PT1H    | 아직 체크인 시작 전입니다. 11월 26일 14:00부터(출발 24시간 전) 체크인할 수 있습니다.",
            "PT48H | PT1H30M | 아직 체크인 시작 전입니다. 11월 26일 14:00부터(출발 48시간 전) 체크인할 수 있습니다.",
            "PT3H  | PT45M   | 아직 체크인 시작 전입니다. 11월 26일 14:00부터(출발 3시간 전) 체크인할 수 있습니다."
    })
    void createsNotOpenYetMessageFromSettings(Duration openBefore, Duration closeBefore, String expected) {
        givenDepartureAt(NOW.plus(openBefore).plusHours(2));

        assertMessage(createService(openBefore, closeBefore), expected);
    }

    @ParameterizedTest(name = "{0} 전 ~ {1} 전 → {2}")
    @DisplayName("체크인이 마감되었으면 설정한 마감 시간과 카운터 문의를 안내한다")
    @CsvSource(delimiter = '|', value = {
            "PT24H | PT1H    | 체크인이 마감되었습니다(출발 1시간 전 마감). 항공사 카운터로 문의해 주세요.",
            "PT48H | PT1H30M | 체크인이 마감되었습니다(출발 1시간 30분 전 마감). 항공사 카운터로 문의해 주세요.",
            "PT3H  | PT45M   | 체크인이 마감되었습니다(출발 45분 전 마감). 항공사 카운터로 문의해 주세요."
    })
    void createsClosedMessageFromSettings(Duration openBefore, Duration closeBefore, String expected) {
        givenDepartureAt(NOW.plusMinutes(30));

        assertMessage(createService(openBefore, closeBefore), expected);
    }

    @ParameterizedTest(name = "출발 {0}초 전 → {1}")
    @DisplayName("경계값: 시작 1초 전은 시작 전 안내, 마감 1초 뒤와 출발 후는 마감 안내")
    @CsvSource(delimiter = '|', value = {
            "86401 | 아직 체크인 시작 전입니다. 11월 26일 12:01부터(출발 24시간 전) 체크인할 수 있습니다.",
            "3599  | 체크인이 마감되었습니다(출발 1시간 전 마감). 항공사 카운터로 문의해 주세요.",
            "-3600 | 체크인이 마감되었습니다(출발 1시간 전 마감). 항공사 카운터로 문의해 주세요."
    })
    void selectsMessageAtBoundary(long secondsBeforeDeparture, String expected) {
        givenDepartureAt(NOW.plusSeconds(secondsBeforeDeparture));

        assertMessage(checkinService, expected);
    }

    // 내림으로 표시하면 안내한 시각에 와도 아직 열리기 전이라 거절된다
    @ParameterizedTest(name = "시작 시각 {0} → {1}")
    @DisplayName("시작 시각에 초가 있으면 분 단위로 올리고, 정각이면 그대로 안내한다")
    @CsvSource(delimiter = '|', value = {
            "2026-11-26T15:00:30 | 11월 26일 15:01",
            "2026-11-26T15:00:00 | 11월 26일 15:00"
    })
    void roundsUpOpeningTimeToMinute(LocalDateTime opensAt, String expectedTime) {
        givenDepartureAt(opensAt.plusHours(24));

        assertMessage(checkinService,
                "아직 체크인 시작 전입니다. " + expectedTime + "부터(출발 24시간 전) 체크인할 수 있습니다.");
    }

    private CheckinService createService(Duration openBefore, Duration closeBefore) {
        return new CheckinServiceImpl(airlineClient, checkinMapper,
                Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL), openBefore, closeBefore);
    }

    private void givenDepartureAt(LocalDateTime depTime) {
        when(airlineClient.findReservation("ABC123", "KIM")).thenReturn(new AirlineReservation(
                "ABC123", "KIM", "MINSU", 23, 1L, "ZZ101", "ICN", "NRT", depTime, "12"));
    }

    private void assertMessage(CheckinService service, String expected) {
        assertThatThrownBy(() -> service.lookupReservation("ABC123", "KIM"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHECKIN_NOT_AVAILABLE);
                    assertThat(e.getMessage()).isEqualTo(expected);
                });
    }

    private void assertErrorCode(ErrorCode expected) {
        assertThatThrownBy(() -> checkinService.lookupReservation("ABC123", "KIM"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
