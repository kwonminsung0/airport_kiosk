package com.example.airport_kiosk.common.exception;

import com.example.airport_kiosk.common.response.ErrorResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 키오스크 API의 예외를 { code, message } 응답으로 바꾼다. 가상 항공사 API에는 적용하지 않는다.
 * Spring 표준 예외(파라미터 누락, 타입 오류, 415 등)의 HTTP 상태는 부모 클래스가 정한다.
 */
@RestControllerAdvice(basePackages = {
        "com.example.airport_kiosk.checkin",
        "com.example.airport_kiosk.bagdrop"
})
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException e) {
        return toResponse(e.getErrorCode(), e.getMessage());
    }

    // 예상하지 못한 오류. 내부 정보가 응답에 나가지 않도록 실제 내용은 서버 로그에만 남긴다
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류", e);
        return toResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    // @Valid 검증 실패. 같은 문구는 한 번만, 항상 같은 순서로 보여 준다
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .sorted()
                .collect(Collectors.joining(" "));
        return toResponse(ErrorCode.INVALID_INPUT, message);
    }

    // 나머지 Spring 표준 예외. 요청이 잘못된 4xx는 그 상태로, 5xx는 예상하지 못한 오류로 응답한다
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (!status.is4xxClientError()) {
            return handleUnexpected(e);
        }
        String code = status.value() == HttpStatus.BAD_REQUEST.value()
                ? ErrorCode.INVALID_INPUT.name()
                : HttpStatus.valueOf(status.value()).name();
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(code, ErrorCode.INVALID_INPUT.getMessage()));
    }

    // 형식을 JSON으로 정해 두지 않으면, JSON을 받지 않는 요청에서 응답을 쓰지 못해 원인 예외가 서버 로그에 그대로 남는다
    private ResponseEntity<Object> toResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(errorCode.name(), message));
    }
}
