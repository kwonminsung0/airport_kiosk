package com.example.airport_kiosk.checkin;

import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * GlobalExceptionHandler 테스트용 컨트롤러.
 * checkin 패키지에 있어야 적용 대상이 되며, @TestComponent를 붙여 다른 테스트에 등록되지 않게 한다.
 */
@RestController
@TestComponent
public class ErrorTestController {

    @GetMapping("/test/errors/{code}")
    public void fail(@PathVariable("code") ErrorCode code) {
        throw new BusinessException(code);
    }

    @GetMapping("/test/unexpected")
    public void unexpected() {
        throw new IllegalStateException("ORA-00942: SECRET_TABLE");
    }

    @PostMapping("/test/validate")
    public void validate(@Valid @RequestBody Input input) {
    }

    @Getter
    @NoArgsConstructor
    public static class Input {

        @NotBlank(message = "나 항목을 확인해 주세요.")
        private String second;

        @NotBlank(message = "가 항목을 확인해 주세요.")
        @Pattern(regexp = "[A-Z]{3}", message = "가 항목을 확인해 주세요.")
        private String first;
    }
}
