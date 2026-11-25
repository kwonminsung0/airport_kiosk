package com.example.airport_kiosk.checkin.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.airport_kiosk.checkin.dto.ReservationLookupResponse;
import com.example.airport_kiosk.checkin.service.CheckinService;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CheckinController.class)
class CheckinControllerTest {

    private static final String PNR_MESSAGE = "예약번호는 영문·숫자 6자리로 입력해 주세요.";
    private static final String LAST_NAME_MESSAGE = "성은 영문으로 입력해 주세요.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckinService checkinService;

    @Test
    @DisplayName("예약 조회에 성공하면 200과 예약 정보를 응답한다")
    void returnsReservation() throws Exception {
        givenReservation();

        lookup("{\"pnr\":\"ABC123\",\"lastName\":\"KIM\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("ABC123"))
                .andExpect(jsonPath("$.firstName").value("MINSU"))
                .andExpect(jsonPath("$.flightNo").value("ZZ101"))
                .andExpect(jsonPath("$.depTime").value("2026-11-26T17:00:00"));
    }

    @Test
    @DisplayName("예약번호는 앞뒤 공백을 지우고 대문자로, 성은 앞뒤 공백만 지워서 조회한다")
    void normalizesInput() throws Exception {
        givenReservation();

        lookup("{\"pnr\":\" abc123 \",\"lastName\":\" Kim \"}").andExpect(status().isOk());

        verify(checkinService).lookupReservation("ABC123", "Kim");
    }

    @ParameterizedTest
    @DisplayName("성에는 공백, 하이픈, 작은따옴표를 쓸 수 있다")
    @ValueSource(strings = {"O'NEIL", "VAN DER BERG", "KIM-LEE"})
    void allowsSpecialLastName(String lastName) throws Exception {
        givenReservation();

        lookup("{\"pnr\":\"ABC123\",\"lastName\":\"" + lastName + "\"}").andExpect(status().isOk());
    }

    @ParameterizedTest
    @DisplayName("예약번호 형식이 틀리면 400과 예약번호 안내를 응답한다")
    @ValueSource(strings = {"null", "\"\"", "\"   \"", "\"ABC12\"", "\"ABC1234\"", "\"ABC-12\"", "\"가나다123\""})
    void rejectsInvalidPnr(String pnr) throws Exception {
        lookup("{\"pnr\":" + pnr + ",\"lastName\":\"KIM\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(PNR_MESSAGE));
        verifyNoInteractions(checkinService);
    }

    @ParameterizedTest
    @DisplayName("성의 형식이 틀리면 400과 성 안내를 응답한다")
    @ValueSource(strings = {"null", "\"\"", "\"   \"", "\"김\"", "\"KIM1\"", "\"-KIM\""})
    void rejectsInvalidLastName(String lastName) throws Exception {
        lookup("{\"pnr\":\"ABC123\",\"lastName\":" + lastName + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(LAST_NAME_MESSAGE));
        verifyNoInteractions(checkinService);
    }

    @Test
    @DisplayName("성이 50자를 넘으면 400을 응답한다")
    void rejectsTooLongLastName() throws Exception {
        lookup("{\"pnr\":\"ABC123\",\"lastName\":\"" + "A".repeat(51) + "\"}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예약번호와 성을 모두 보내지 않으면 두 안내를 함께 응답한다")
    void rejectsEmptyObject() throws Exception {
        lookup("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(LAST_NAME_MESSAGE + " " + PNR_MESSAGE));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("서비스의 오류를 정해진 상태와 문구로 응답한다")
    @CsvSource({
            "RESERVATION_NOT_FOUND, 404",
            "ALREADY_CHECKED_IN, 409",
            "CHECKIN_NOT_AVAILABLE, 422",
            "AIRLINE_ERROR, 502",
            "AIRLINE_TIMEOUT, 504"
    })
    void respondsWithServiceError(ErrorCode code, int expectedStatus) throws Exception {
        when(checkinService.lookupReservation(anyString(), anyString())).thenThrow(new BusinessException(code));

        lookup("{\"pnr\":\"ABC123\",\"lastName\":\"KIM\"}")
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value(code.getMessage()));
    }

    private void givenReservation() {
        when(checkinService.lookupReservation(anyString(), anyString())).thenReturn(new ReservationLookupResponse(
                "ABC123", "KIM", "MINSU", 1L, "ZZ101", "ICN", "NRT", LocalDateTime.of(2026, 11, 26, 17, 0), "12"));
    }

    private ResultActions lookup(String body) throws Exception {
        return mockMvc.perform(post("/api/checkin/reservations/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
