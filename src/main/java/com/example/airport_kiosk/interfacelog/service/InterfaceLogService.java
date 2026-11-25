package com.example.airport_kiosk.interfacelog.service;

public interface InterfaceLogService {

    /**
     * 연계 이력을 별도 트랜잭션으로 저장한다.
     *
     * @param requestSummary 요청 요약 (이름 같은 개인정보는 넣지 않는다)
     */
    void save(String interfaceName, String requestSummary, String resultCode, boolean success, long elapsedMs);
}
