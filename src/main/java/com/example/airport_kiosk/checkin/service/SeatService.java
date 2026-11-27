package com.example.airport_kiosk.checkin.service;

import com.example.airport_kiosk.checkin.dto.SeatMapResponse;

public interface SeatService {

    /** 항공편의 좌석 배치도와 좌석별 상태를 조회한다 */
    SeatMapResponse getSeatMap(Long flightId);

    /** 좌석을 예약번호에 배정한다. 체크인(4단계)의 트랜잭션 안에서 호출한다 */
    void assignSeat(Long flightId, Long seatId, String pnr);
}
