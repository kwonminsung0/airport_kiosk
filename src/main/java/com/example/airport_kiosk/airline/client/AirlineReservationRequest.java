package com.example.airport_kiosk.airline.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 키오스크가 항공사 시스템에 보내는 예약 조회 요청 본문 */
@Getter
@AllArgsConstructor
public class AirlineReservationRequest {

    private String pnr;
    private String lastName;
}
