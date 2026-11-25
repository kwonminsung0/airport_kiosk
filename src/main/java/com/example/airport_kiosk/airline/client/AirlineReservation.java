package com.example.airport_kiosk.airline.client;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 항공사 시스템에서 받은 예약과 항공편 정보 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AirlineReservation {

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
