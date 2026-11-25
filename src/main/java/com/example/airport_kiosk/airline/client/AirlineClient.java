package com.example.airport_kiosk.airline.client;

import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import com.example.airport_kiosk.interfacelog.service.InterfaceLogService;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** 키오스크가 항공사 시스템을 HTTP로 호출하는 클라이언트. 호출할 때마다 연계 로그를 남긴다 */
@Component
@RequiredArgsConstructor
public class AirlineClient {

    private static final Logger log = LoggerFactory.getLogger(AirlineClient.class);
    private static final String INTERFACE_NAME = "AIRLINE_RESERVATION";

    private final RestClient airlineRestClient;
    private final InterfaceLogService interfaceLogService;

    public AirlineReservation findReservation(String pnr, String lastName) {
        long startedAt = System.nanoTime();
        String resultCode = "ERROR";
        boolean success = false;
        try {
            AirlineReservation reservation = airlineRestClient.get()
                    .uri("/reservations?pnr={pnr}&lastName={lastName}", pnr, lastName)
                    .retrieve()
                    .body(AirlineReservation.class);
            if (reservation == null) {
                throw new BusinessException(ErrorCode.AIRLINE_ERROR);
            }
            resultCode = String.valueOf(HttpStatus.OK.value());
            success = true;
            return reservation;
        } catch (HttpClientErrorException.NotFound e) {
            // 통신은 정상이고 예약만 없는 경우이므로 연계는 성공으로 기록한다
            resultCode = String.valueOf(HttpStatus.NOT_FOUND.value());
            success = true;
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, e);
        } catch (RestClientResponseException e) {
            resultCode = String.valueOf(e.getStatusCode().value());
            throw new BusinessException(ErrorCode.AIRLINE_ERROR, e);
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                resultCode = "TIMEOUT";
                throw new BusinessException(ErrorCode.AIRLINE_TIMEOUT, e);
            }
            throw new BusinessException(ErrorCode.AIRLINE_ERROR, e);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            saveLog(pnr, resultCode, success, elapsedMs);
        }
    }

    private boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    // 로그 저장에 실패해도 승객의 업무는 계속한다. 이름은 개인정보이므로 남기지 않는다
    private void saveLog(String pnr, String resultCode, boolean success, long elapsedMs) {
        try {
            interfaceLogService.save(INTERFACE_NAME, "pnr=" + pnr, resultCode, success, elapsedMs);
        } catch (RuntimeException e) {
            log.error("연계 로그 저장 실패: interfaceName={}, resultCode={}", INTERFACE_NAME, resultCode, e);
        }
    }
}
