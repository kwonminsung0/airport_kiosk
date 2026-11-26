package com.example.airport_kiosk.airline.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import com.example.airport_kiosk.interfacelog.service.InterfaceLogService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

class AirlineClientTest {

    // 성이 주소(쿼리 문자열)에 남지 않도록 주소에는 값을 넣지 않는다
    private static final String URL = "http://airline.test/reservations/search";
    private static final String BODY = """
            {"pnr":"ABC123","lastName":"KIM","firstName":"MINSU","baggageAllowanceKg":23,"flightId":1,
             "flightNo":"ZZ101","depAirport":"ICN","arrAirport":"NRT","depTime":"2026-11-25T22:45:55","gate":"12"}
            """;

    private final InterfaceLogService interfaceLogService = mock(InterfaceLogService.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(AirlineClient.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private MockRestServiceServer server;
    private AirlineClient airlineClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://airline.test");
        server = MockRestServiceServer.bindTo(builder).build();
        airlineClient = new AirlineClient(builder.build(), interfaceLogService);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void removeAppender() {
        logger.detachAppender(logs);
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

    @ParameterizedTest(name = "{0}")
    @DisplayName("응답의 Content-Type 형식이 잘못되면 AIRLINE_ERROR가 발생하고 실패로 기록한다")
    @ValueSource(strings = {"not-a-type", "application/", "application/json; charset=nope"})
    void throwsAirlineErrorOnInvalidContentType(String contentType) {
        assertErrorCode(withContentType(contentType), ErrorCode.AIRLINE_ERROR);
        verifyLog("ERROR", false);
    }

    // 형식은 맞지만 해석할 수 없는 Content-Type. RestClientException으로 처리되는 경우가 바뀌지 않게 남겨 둔다
    @ParameterizedTest(name = "{0}")
    @DisplayName("응답의 Content-Type을 해석할 수 없으면 AIRLINE_ERROR가 발생하고 실패로 기록한다")
    @ValueSource(strings = {"text/html"})
    void throwsAirlineErrorOnUnreadableContentType(String contentType) {
        assertErrorCode(withContentType(contentType), ErrorCode.AIRLINE_ERROR);
        verifyLog("ERROR", false);
    }

    @Test
    @DisplayName("Content-Type 형식이 잘못되면 예외 종류만 남기고 헤더 값은 남기지 않는다")
    void logsInvalidContentTypeWithoutValue() {
        assertErrorCode(withContentType("application/json; charset=SECRETBODY"), ErrorCode.AIRLINE_ERROR);

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(loggedText()).contains("InvalidMediaTypeException").doesNotContain("SECRETBODY", "KIM");
    }

    @Test
    @DisplayName("응답 본문이 비어 있으면 AIRLINE_ERROR가 발생하고 실패로 기록한다")
    void throwsAirlineErrorOnEmptyBody() {
        assertErrorCode(withSuccess(), ErrorCode.AIRLINE_ERROR);
        verifyLog("ERROR", false);
    }

    @Test
    @DisplayName("연결에 실패하면 원인 예외를 서버 로그에 남기고 성은 남기지 않는다")
    void logsConnectionFailureCause() {
        assertErrorCode(withException(new ConnectException("Connection refused")), ErrorCode.AIRLINE_ERROR);

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(loggedText()).contains("ERROR", "ConnectException: Connection refused").doesNotContain("KIM");
    }

    @Test
    @DisplayName("응답이 늦으면 원인 예외를 서버 로그에 남기고 성은 남기지 않는다")
    void logsTimeoutCause() {
        assertErrorCode(withException(new SocketTimeoutException("Read timed out")), ErrorCode.AIRLINE_TIMEOUT);

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(loggedText()).contains("TIMEOUT", "SocketTimeoutException: Read timed out").doesNotContain("KIM");
    }

    @Test
    @DisplayName("항공사 서버 오류면 상태 코드만 남기고 응답 본문은 남기지 않는다")
    void logsStatusWithoutBody() {
        assertErrorCode(withServerError().body("error for KIM"), ErrorCode.AIRLINE_ERROR);

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(loggedText()).contains("500", "InternalServerError").doesNotContain("KIM");
    }

    @Test
    @DisplayName("응답을 해석할 수 없으면 예외 종류만 남기고 응답 본문은 남기지 않는다")
    void logsParseFailureWithoutBody() {
        assertErrorCode(withSuccess("{\"flightId\":\"KIM\"}", MediaType.APPLICATION_JSON), ErrorCode.AIRLINE_ERROR);

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(loggedText()).contains("InvalidFormatException").doesNotContain("KIM");
    }

    @Test
    @DisplayName("응답 본문이 비어 있으면 그 사실을 서버 로그에 남긴다")
    void logsEmptyBody() {
        assertErrorCode(withSuccess(), ErrorCode.AIRLINE_ERROR);

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(loggedText()).contains("본문이 비어 있음");
    }

    @Test
    @DisplayName("예약이 없는 것은 장애가 아니므로 서버 로그에 경고를 남기지 않는다")
    void doesNotWarnOnNotFound() {
        assertErrorCode(withResourceNotFound(), ErrorCode.RESERVATION_NOT_FOUND);

        assertThat(logs.list).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("응답을 받은 뒤의 오류는 원인 예외를 담지 않아 응답 본문이 예외에 남지 않는다")
    @MethodSource("responsesWithName")
    void doesNotKeepCauseAfterResponse(String name, ResponseCreator response) {
        BusinessException e = catchBusinessException(response);

        assertThat(e.getCause()).isNull();
        assertThat(stackTraceOf(e)).doesNotContain("SECRETBODY");
    }

    static Stream<Arguments> responsesWithName() {
        return Stream.of(
                Arguments.of("404", withResourceNotFound().body("SECRETBODY")),
                Arguments.of("400", withBadRequest().body("SECRETBODY")),
                Arguments.of("500", withServerError().body("SECRETBODY")),
                Arguments.of("JSON 아님", withSuccess("SECRETBODY", MediaType.APPLICATION_JSON)),
                Arguments.of("타입이 다른 JSON", withSuccess("{\"flightId\":\"SECRETBODY\"}", MediaType.APPLICATION_JSON)),
                Arguments.of("잘못된 Content-Type", withContentType("application/json; charset=SECRETBODY")));
    }

    // 지정한 Content-Type 헤더를 가진 200 응답
    private static ResponseCreator withContentType(String contentType) {
        return withSuccess().body(BODY).header("Content-Type", contentType);
    }

    @Test
    @DisplayName("본문을 읽다가 늦어지면 AIRLINE_TIMEOUT이 발생하고 원인 예외는 담지 않는다")
    void throwsTimeoutWithoutCauseWhenBodyIsSlow() {
        BusinessException e = catchBusinessException(respondWithSlowBody());

        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AIRLINE_TIMEOUT);
        assertThat(e.getCause()).isNull();
        assertThat(stackTraceOf(e)).doesNotContain("SECRETBODY");
        assertThat(loggedText()).contains("TIMEOUT", "SocketTimeoutException").doesNotContain("SECRETBODY", "KIM");
        verifyLog("TIMEOUT", false);
    }

    @Test
    @DisplayName("통신 실패는 응답 본문이 없으므로 원인 예외를 담는다")
    void keepsCauseOnCommunicationFailure() {
        BusinessException connect = catchBusinessException(withException(new ConnectException("Connection refused")));
        assertThat(connect).hasRootCauseInstanceOf(ConnectException.class);

        server.reset();
        BusinessException timeout = catchBusinessException(withException(new SocketTimeoutException("Read timed out")));
        assertThat(timeout).hasRootCauseInstanceOf(SocketTimeoutException.class);
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
    @DisplayName("예약번호와 성은 주소가 아니라 POST 요청 본문에 JSON으로 담아 보낸다")
    void sendsLastNameInBody() {
        expectSearch("ABC123", "KIM").andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        airlineClient.findReservation("ABC123", "KIM");

        server.verify();
    }

    @Test
    @DisplayName("성에 특수문자가 있어도 본문에 그대로 담아 보낸다")
    void sendsSpecialCharactersInBody() {
        expectSearch("ABC123", "O'NEIL&x=1").andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        airlineClient.findReservation("ABC123", "O'NEIL&x=1");

        server.verify();
    }

    // 주소에 쿼리 문자열이 없고, POST 본문이 정확히 { pnr, lastName }인 요청만 받는다
    private ResponseActions expectSearch(String pnr, String lastName) {
        String body = "{\"pnr\":\"" + pnr + "\",\"lastName\":\"" + lastName + "\"}";
        return server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(body, true));
    }

    // 로그 문구와 예외 내용(원인 포함)을 합친 문자열
    private String loggedText() {
        return logs.list.stream()
                .map(event -> event.getFormattedMessage() + ThrowableProxyUtil.asString(event.getThrowableProxy()))
                .collect(Collectors.joining());
    }

    // 본문의 앞부분만 보낸 뒤 시간 초과가 나는 응답
    private ResponseCreator respondWithSlowBody() {
        InputStream firstPart = new ByteArrayInputStream("{\"lastName\":\"SECRETBODY\",".getBytes(StandardCharsets.UTF_8));
        InputStream timeout = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new SocketTimeoutException("Read timed out");
            }
        };
        return request -> {
            MockClientHttpResponse response =
                    new MockClientHttpResponse(new SequenceInputStream(firstPart, timeout), HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        };
    }

    private BusinessException catchBusinessException(ResponseCreator response) {
        server.expect(requestTo(URL)).andRespond(response);
        return catchThrowableOfType(BusinessException.class, () -> airlineClient.findReservation("ABC123", "KIM"));
    }

    // 예외가 처리되지 않았을 때 서버가 기록하는 내용 (원인 포함)
    private String stackTraceOf(Throwable e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
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
