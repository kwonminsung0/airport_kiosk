package com.example.airport_kiosk.airline.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import com.example.airport_kiosk.interfacelog.service.InterfaceLogService;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

class AirlineClientTest {

    private static final String URL = "http://airline.test/reservations?pnr=ABC123&lastName=KIM";
    private static final String BODY = """
            {"pnr":"ABC123","lastName":"KIM","firstName":"MINSU","baggageAllowanceKg":23,"flightId":1,
             "flightNo":"ZZ101","depAirport":"ICN","arrAirport":"NRT","depTime":"2026-11-25T22:45:55","gate":"12"}
            """;

    private final InterfaceLogService interfaceLogService = mock(InterfaceLogService.class);
    private MockRestServiceServer server;
    private AirlineClient airlineClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://airline.test");
        server = MockRestServiceServer.bindTo(builder).build();
        airlineClient = new AirlineClient(builder.build(), interfaceLogService);
    }

    @Test
    @DisplayName("예약 조회에 성공하면 예약을 돌려주고 성공 로그를 남긴다")
    void returnsReservation() {
        server.expect(requestTo(URL)).andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        AirlineReservation reservation = airlineClient.findReservation("ABC123", "KIM");

        assertThat(reservation.getPnr()).isEqualTo("ABC123");
        assertThat(reservation.getFlightNo()).isEqualTo("ZZ101");
        assertThat(reservation.getBaggageAllowanceKg()).isEqualTo(23);
        assertThat(reservation.getDepTime()).isEqualTo(LocalDateTime.of(2026, 11, 25, 22, 45, 55));
        verifyLog("200", true);
    }

    @Test
    @DisplayName("예약이 없으면 RESERVATION_NOT_FOUND가 발생하고 연계는 성공으로 기록한다")
    void throwsNotFound() {
        assertErrorCode(withResourceNotFound(), ErrorCode.RESERVATION_NOT_FOUND);
        verifyLog("404", true);
    }

    @Test
    @DisplayName("항공사 서버 오류면 AIRLINE_ERROR가 발생하고 실패로 기록한다")
    void throwsAirlineErrorOnServerError() {
        assertErrorCode(withServerError(), ErrorCode.AIRLINE_ERROR);
        verifyLog("500", false);
    }

    @Test
    @DisplayName("응답이 늦으면 AIRLINE_TIMEOUT이 발생하고 실패로 기록한다")
    void throwsTimeout() {
        assertErrorCode(withException(new SocketTimeoutException("Read timed out")), ErrorCode.AIRLINE_TIMEOUT);
        verifyLog("TIMEOUT", false);
    }

    @Test
    @DisplayName("연결할 수 없으면 AIRLINE_ERROR가 발생하고 실패로 기록한다")
    void throwsAirlineErrorOnConnectionFailure() {
        assertErrorCode(withException(new ConnectException("Connection refused")), ErrorCode.AIRLINE_ERROR);
        verifyLog("ERROR", false);
    }

    @Test
    @DisplayName("응답 본문을 해석할 수 없으면 AIRLINE_ERROR가 발생하고 실패로 기록한다")
    void throwsAirlineErrorOnInvalidBody() {
        assertErrorCode(withSuccess("not json", MediaType.APPLICATION_JSON), ErrorCode.AIRLINE_ERROR);
        verifyLog("ERROR", false);
    }

    @Test
    @DisplayName("응답 본문이 비어 있으면 AIRLINE_ERROR가 발생하고 실패로 기록한다")
    void throwsAirlineErrorOnEmptyBody() {
        assertErrorCode(withSuccess(), ErrorCode.AIRLINE_ERROR);
        verifyLog("ERROR", false);
    }

    @Test
    @DisplayName("로그 저장에 실패해도 예약은 돌려준다")
    void returnsReservationEvenIfLogFails() {
        server.expect(requestTo(URL)).andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
        doThrow(new DataAccessResourceFailureException("DB 연결 실패"))
                .when(interfaceLogService).save(anyString(), anyString(), anyString(), anyBoolean(), anyLong());

        assertThat(airlineClient.findReservation("ABC123", "KIM").getPnr()).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("성에 특수문자가 있어도 인코딩해서 보낸다")
    void encodesLastName() {
        server.expect(requestTo("http://airline.test/reservations?pnr=ABC123&lastName=O%27NEIL%26x%3D1"))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        airlineClient.findReservation("ABC123", "O'NEIL&x=1");

        server.verify();
    }

    private void assertErrorCode(ResponseCreator response, ErrorCode expected) {
        server.expect(requestTo(URL)).andRespond(response);

        assertThatThrownBy(() -> airlineClient.findReservation("ABC123", "KIM"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    // 요청 요약에 PNR만 남고 이름은 남지 않는 것도 함께 확인한다
    private void verifyLog(String resultCode, boolean success) {
        verify(interfaceLogService)
                .save(eq("AIRLINE_RESERVATION"), eq("pnr=ABC123"), eq(resultCode), eq(success), anyLong());
    }
}
