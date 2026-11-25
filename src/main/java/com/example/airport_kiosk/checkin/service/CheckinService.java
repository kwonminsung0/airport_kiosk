package com.example.airport_kiosk.checkin.service;

import com.example.airport_kiosk.checkin.dto.ReservationLookupResponse;

public interface CheckinService {

    /** 예약을 조회하고 지금 체크인할 수 있는지 확인한다 */
    ReservationLookupResponse lookupReservation(String pnr, String lastName);
}
