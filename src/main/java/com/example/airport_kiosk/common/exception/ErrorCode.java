package com.example.airport_kiosk.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다. 예약번호와 성을 확인해 주세요."),
    FLIGHT_NOT_FOUND(HttpStatus.NOT_FOUND, "항공편을 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "이미 체크인한 예약입니다."),
    SEAT_ALREADY_TAKEN(HttpStatus.CONFLICT, "이미 선택된 좌석입니다. 다른 좌석을 선택해 주세요."),
    CHECKIN_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "지금은 체크인할 수 있는 시간이 아닙니다."),
    AIRLINE_ERROR(HttpStatus.BAD_GATEWAY, "항공사 시스템에 문제가 있습니다. 잠시 후 다시 시도하거나 카운터로 문의해 주세요."),
    AIRLINE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "항공사 시스템의 응답이 늦습니다. 잠시 후 다시 시도하거나 카운터로 문의해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;
}
