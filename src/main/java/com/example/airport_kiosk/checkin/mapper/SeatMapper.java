package com.example.airport_kiosk.checkin.mapper;

import com.example.airport_kiosk.checkin.vo.FlightVO;
import com.example.airport_kiosk.checkin.vo.SeatVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeatMapper {

    FlightVO findFlightById(@Param("flightId") Long flightId);

    List<SeatVO> findByFlightId(@Param("flightId") Long flightId);

    SeatVO findSeatById(@Param("seatId") Long seatId);

    /** 이 항공편의 빈 좌석일 때만 배정한다. 바뀐 행 수(1 또는 0)를 돌려준다 */
    int assignSeat(@Param("flightId") Long flightId, @Param("seatId") Long seatId, @Param("pnr") String pnr);
}
