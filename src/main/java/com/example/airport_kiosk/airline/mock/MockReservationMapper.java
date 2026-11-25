package com.example.airport_kiosk.airline.mock;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MockReservationMapper {

    MockReservationResponse findReservation(@Param("pnr") String pnr, @Param("lastName") String lastName);
}
