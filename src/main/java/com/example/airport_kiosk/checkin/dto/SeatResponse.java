package com.example.airport_kiosk.checkin.dto;

import com.example.airport_kiosk.checkin.vo.SeatVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 좌석 하나. 누가 앉았는지(pnr)는 다른 승객에게 보이지 않도록 담지 않는다 */
@Getter
@AllArgsConstructor
public class SeatResponse {

    private Long seatId;
    private String seatNo;
    private String status;

    public static SeatResponse from(SeatVO seat) {
        return new SeatResponse(seat.getSeatId(), seat.getSeatNo(), seat.getStatus());
    }
}
