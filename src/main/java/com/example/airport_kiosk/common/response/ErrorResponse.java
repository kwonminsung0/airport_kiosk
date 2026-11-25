package com.example.airport_kiosk.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 공통 에러 응답 */
@Getter
@AllArgsConstructor
public class ErrorResponse {

    private String code;
    private String message;
}
