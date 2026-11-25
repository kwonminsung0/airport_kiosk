package com.example.airport_kiosk.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.airport_kiosk.checkin.ErrorTestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
