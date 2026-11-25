package com.example.airport_kiosk.checkin.dto;

import com.example.airport_kiosk.airline.client.AirlineReservation;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 예약 조회 응답 */
@Getter
@AllArgsConstructor
public class ReservationLookupResponse {

    private String pnr;
    private String lastName;
    private String firstName;
    private Long flightId;
    private String flightNo;
    private String depAirport;
    private String arrAirport;
    private LocalDateTime depTime;
    private String gate;

    public static ReservationLookupResponse from(AirlineReservation reservation) {
        return new ReservationLookupResponse(
                reservation.getPnr(),
                reservation.getLastName(),
                reservation.getFirstName(),
                reservation.getFlightId(),
                reservation.getFlightNo(),
                reservation.getDepAirport(),
                reservation.getArrAirport(),
                reservation.getDepTime(),
                reservation.getGate());
    }
}
