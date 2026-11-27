package com.example.airport_kiosk.checkin.service.impl;

import com.example.airport_kiosk.checkin.dto.SeatMapResponse;
import com.example.airport_kiosk.checkin.dto.SeatResponse;
import com.example.airport_kiosk.checkin.mapper.SeatMapper;
import com.example.airport_kiosk.checkin.service.SeatService;
import com.example.airport_kiosk.checkin.vo.FlightVO;
import com.example.airport_kiosk.checkin.vo.SeatVO;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatMapper seatMapper;

    // 항공편이 없을 때 빈 좌석 목록이 아니라 404로 알려 주려고 항공편을 먼저 조회한다
    @Override
    public SeatMapResponse getSeatMap(Long flightId) {
        FlightVO flight = seatMapper.findFlightById(flightId);
        if (flight == null) {
            throw new BusinessException(ErrorCode.FLIGHT_NOT_FOUND);
        }
        List<SeatResponse> seats = seatMapper.findByFlightId(flightId).stream()
                .map(SeatResponse::from)
                .toList();
        return new SeatMapResponse(flight.getFlightId(), flight.getSeatRows(), flight.getSeatCols(), seats);
    }

    // 먼저 조회하고 비어 있으면 UPDATE하면, 두 사람이 동시에 "비어 있음"을 보고 둘 다 배정할 수 있다
    // 그래서 확인과 변경을 조건부 UPDATE 한 문장으로 하고, 바뀐 행 수로 성공 여부를 판단한다
    @Override
    @Transactional
    public void assignSeat(Long flightId, Long seatId, String pnr) {
        if (seatMapper.assignSeat(flightId, seatId, pnr) == 1) {
            return;
        }
        // 0행이면 이유를 나눈다: 이 항공편에 없는 좌석(404)인지, 이미 배정된 좌석(409)인지
        SeatVO seat = seatMapper.findSeatById(seatId);
        if (seat == null || !seat.getFlightId().equals(flightId)) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND);
        }
        throw new BusinessException(ErrorCode.SEAT_ALREADY_TAKEN);
    }
}
