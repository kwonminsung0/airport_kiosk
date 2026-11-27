package com.example.airport_kiosk.checkin.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** FLIGHT 테이블의 한 행 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FlightVO {

    private Long flightId;
    private String flightNo;
    private String depAirport;
    private String arrAirport;
    private LocalDateTime depTime;
    private String gate;
    private int seatRows;
    private int seatCols;
}
