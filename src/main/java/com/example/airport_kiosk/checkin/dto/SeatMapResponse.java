package com.example.airport_kiosk.checkin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 좌석 배치도. 화면은 행·열 수로 격자를 그리고, 통로 위치는 열 수의 가운데로 계산한다 */
@Getter
@AllArgsConstructor
public class SeatMapResponse {

    private Long flightId;
    private int seatRows;
    private int seatCols;
    private List<SeatResponse> seats;
}
