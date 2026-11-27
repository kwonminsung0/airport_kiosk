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
}
