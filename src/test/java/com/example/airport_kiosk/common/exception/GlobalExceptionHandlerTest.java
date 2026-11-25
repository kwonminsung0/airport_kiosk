package com.example.airport_kiosk.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.airport_kiosk.checkin.ErrorTestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ErrorTestController(), new OutsideController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("BusinessException은 ErrorCode의 상태와 문구로 응답한다")
    @CsvSource({
            "RESERVATION_NOT_FOUND, 404",
            "ALREADY_CHECKED_IN, 409",
            "CHECKIN_NOT_AVAILABLE, 422",
            "AIRLINE_ERROR, 502",
            "AIRLINE_TIMEOUT, 504"
    })
    void respondsWithErrorCode(ErrorCode code, int expectedStatus) throws Exception {
        mockMvc.perform(get("/test/errors/" + code))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value(code.getMessage()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("BusinessException에 문구를 직접 넣으면 그 문구로 응답한다")
    void respondsWithCustomMessage() throws Exception {
        mockMvc.perform(get("/test/custom-message"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("CHECKIN_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("직접 만든 문구"));
    }

    @Test
    @DisplayName("BusinessException의 원인 예외 내용은 응답하지 않는다")
    void hidesCauseMessage() throws Exception {
        mockMvc.perform(get("/test/with-cause"))
                .andExpect(status().is(502))
                .andExpect(jsonPath("$.message").value(ErrorCode.AIRLINE_ERROR.getMessage()));
    }

    @Test
    @DisplayName("입력값 검증에 실패하면 400과 항목별 문구를 한 번씩 정해진 순서로 응답한다")
    void respondsWithValidationMessages() throws Exception {
        mockMvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"first\":\"\",\"second\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("가 항목을 확인해 주세요. 나 항목을 확인해 주세요."));
    }

    @Test
    @DisplayName("요청 본문이 JSON이 아니면 400을 응답한다")
    void respondsBadRequestWhenBodyIsNotJson() throws Exception {
        mockMvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."));
    }

    @Test
    @DisplayName("주소의 값이 정해진 타입이 아니면 400을 응답한다")
    void respondsBadRequestWhenTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/errors/UNKNOWN_CODE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("요청이 잘못된 Spring 표준 예외는 500이 아니라 400으로 응답한다")
    @ValueSource(strings = {
            "/test/param", "/test/header", "/test/min?value=0", "/test/min?value=abc"
    })
    void respondsBadRequestForStandardClientErrors(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("JSON이 아닌 Content-Type은 415로 응답하고 서버 오류로 기록하지 않는다")
    @ValueSource(strings = {"text/plain", "application/x-www-form-urlencoded", "application/octet-stream"})
    void respondsUnsupportedMediaType(String contentType, CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/validate").contentType(contentType).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해 주세요."));

        assertThat(output.getAll()).doesNotContain("예상하지 못한 오류");
    }

    @Test
    @DisplayName("예상하지 못한 오류는 일반 문구만 응답하고 실제 내용은 서버 로그에만 남긴다")
    void hidesInternalErrorDetails(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("ORA-00942", "SECRET_TABLE", "IllegalStateException");
        assertThat(output.getAll()).contains("ORA-00942: SECRET_TABLE");
    }

    @Test
    @DisplayName("Spring 표준 예외라도 서버 오류면 500과 일반 문구로 응답하고 로그에 남긴다")
    void hidesStandardServerErrorDetails(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(get("/test/not-writable"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("SECRET_FIELD");
        assertThat(output.getAll()).contains("SECRET_FIELD");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("JSON을 받지 않는 요청에도 오류는 정해진 상태와 JSON으로 응답한다")
    @CsvSource({
            "/test/errors/RESERVATION_NOT_FOUND, 404, RESERVATION_NOT_FOUND",
            "/test/errors/CHECKIN_NOT_AVAILABLE, 422, CHECKIN_NOT_AVAILABLE",
            "/test/with-cause, 502, AIRLINE_ERROR",
            "/test/param, 400, INVALID_INPUT",
            "/test/unexpected, 500, INTERNAL_ERROR"
    })
    void respondsJsonEvenIfNotAcceptable(String path, int expectedStatus, String expectedCode) throws Exception {
        mockMvc.perform(get(path).accept(MediaType.TEXT_HTML))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @ParameterizedTest(name = "Content-Type: {0} → {1}")
    @DisplayName("JSON을 받지 않는 요청에도 본문 오류는 정해진 상태와 JSON으로 응답한다")
    @CsvSource({
            "application/json, 400, INVALID_INPUT",
            "text/plain, 415, UNSUPPORTED_MEDIA_TYPE"
    })
    void respondsJsonForBodyErrorsEvenIfNotAcceptable(String contentType, int expectedStatus, String expectedCode)
            throws Exception {
        mockMvc.perform(post("/test/validate").accept(MediaType.TEXT_HTML).contentType(contentType).content("{}"))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    @DisplayName("checkin·bagdrop 밖의 컨트롤러에는 적용하지 않는다")
    void doesNotApplyOutsideKioskPackages() {
        assertThatThrownBy(() -> mockMvc.perform(get("/test/outside")))
                .hasCauseInstanceOf(BusinessException.class);
    }

    @RestController
    @TestComponent
    static class OutsideController {

        @GetMapping("/test/outside")
        public void fail() {
            throw new BusinessException(ErrorCode.AIRLINE_ERROR);
        }
    }
}
