package com.example.airport_kiosk.checkin.service.impl;

import com.example.airport_kiosk.checkin.dto.SeatMapResponse;
import com.example.airport_kiosk.checkin.dto.SeatResponse;
import com.example.airport_kiosk.checkin.mapper.SeatMapper;
import com.example.airport_kiosk.checkin.service.SeatService;
import com.example.airport_kiosk.checkin.vo.FlightVO;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
