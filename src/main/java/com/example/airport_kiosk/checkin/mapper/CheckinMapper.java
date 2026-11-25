package com.example.airport_kiosk.checkin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CheckinMapper {

    int countByPnr(@Param("pnr") String pnr);
}
