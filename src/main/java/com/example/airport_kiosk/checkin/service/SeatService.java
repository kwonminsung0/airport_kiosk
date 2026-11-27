package com.example.airport_kiosk.checkin.service;

import com.example.airport_kiosk.checkin.dto.SeatMapResponse;

public interface SeatService {

    /** 항공편의 좌석 배치도와 좌석별 상태를 조회한다 */
    SeatMapResponse getSeatMap(Long flightId);
}
