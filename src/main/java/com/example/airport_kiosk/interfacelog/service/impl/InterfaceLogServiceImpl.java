package com.example.airport_kiosk.interfacelog.service.impl;

import com.example.airport_kiosk.interfacelog.mapper.InterfaceLogMapper;
import com.example.airport_kiosk.interfacelog.service.InterfaceLogService;
import com.example.airport_kiosk.interfacelog.vo.InterfaceLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterfaceLogServiceImpl implements InterfaceLogService {

    private final InterfaceLogMapper interfaceLogMapper;

    // 호출한 쪽의 트랜잭션이 롤백되어도 연계 이력은 남도록 별도 트랜잭션을 사용한다
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String interfaceName, String requestSummary, String resultCode, boolean success, long elapsedMs) {
        String successYn = success ? "Y" : "N";
        // log_id와 created_at은 INSERT 문에서 DB가 채운다
        InterfaceLogVO log = new InterfaceLogVO(
                null, interfaceName, requestSummary, resultCode, successYn, elapsedMs, null);
        interfaceLogMapper.insert(log);
    }
}
