package com.example.airport_kiosk.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다. 예약번호와 성을 확인해 주세요."),
    AIRLINE_ERROR(HttpStatus.BAD_GATEWAY, "항공사 시스템에 문제가 있습니다. 잠시 후 다시 시도하거나 카운터로 문의해 주세요."),
    AIRLINE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "항공사 시스템의 응답이 늦습니다. 잠시 후 다시 시도하거나 카운터로 문의해 주세요.");

    private final HttpStatus status;
    private final String message;
}
