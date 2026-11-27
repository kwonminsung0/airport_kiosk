package com.example.airport_kiosk.checkin.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** SEAT 테이블의 한 행 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatVO {

    private Long seatId;
    private Long flightId;
    private String seatNo;
    private String status;
    private String pnr;
}
