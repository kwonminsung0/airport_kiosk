package com.example.airport_kiosk.interfacelog.mapper;

import com.example.airport_kiosk.interfacelog.vo.InterfaceLogVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterfaceLogMapper {

    int insert(InterfaceLogVO log);
}
