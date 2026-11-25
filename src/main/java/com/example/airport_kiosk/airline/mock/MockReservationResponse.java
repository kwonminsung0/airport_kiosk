package com.example.airport_kiosk.airline.mock;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 가상 항공사 API의 예약 조회 응답 (예약 + 항공편) */
@Getter
@NoArgsConstructor
public class MockReservationResponse {

    private String pnr;
    private String lastName;
    private String firstName;
    private int baggageAllowanceKg;
    private Long flightId;
    private String flightNo;
    private String depAirport;
    private String arrAirport;
    private LocalDateTime depTime;
    private String gate;
}
