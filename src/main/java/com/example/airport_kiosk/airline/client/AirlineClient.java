package com.example.airport_kiosk.airline.client;

import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import com.example.airport_kiosk.interfacelog.service.InterfaceLogService;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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
            // 성이 주소와 접속 기록에 남지 않도록 POST 본문으로 보낸다
            AirlineReservation reservation = airlineRestClient.post()
                    .uri("/reservations/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AirlineReservationRequest(pnr, lastName))
                    .retrieve()
                    .body(AirlineReservation.class);
            if (reservation == null) {
                log.warn("항공사 응답의 본문이 비어 있음: interfaceName={}", INTERFACE_NAME);
                throw new BusinessException(ErrorCode.AIRLINE_ERROR);
            }
            resultCode = String.valueOf(HttpStatus.OK.value());
            success = true;
            return reservation;
        } catch (HttpClientErrorException.NotFound e) {
            // 통신은 정상이고 예약만 없는 경우이므로 연계는 성공으로 기록한다
            resultCode = String.valueOf(HttpStatus.NOT_FOUND.value());
            success = true;
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        } catch (RestClientResponseException e) {
            resultCode = String.valueOf(e.getStatusCode().value());
            logFailure(resultCode, e);
            throw new BusinessException(ErrorCode.AIRLINE_ERROR);
        } catch (RestClientException e) {
            boolean timeout = isTimeout(e);
            if (timeout) {
                resultCode = "TIMEOUT";
            }
            logFailure(resultCode, e);
            ErrorCode errorCode = timeout ? ErrorCode.AIRLINE_TIMEOUT : ErrorCode.AIRLINE_ERROR;
            // 응답을 받은 뒤의 오류는 응답 본문이 들어 있을 수 있어 원인 예외를 담지 않는다
            if (e instanceof ResourceAccessException) {
                throw new BusinessException(errorCode, e);
            }
            throw new BusinessException(errorCode);
        } catch (InvalidMediaTypeException e) {
            // 응답의 Content-Type 형식이 잘못된 경우. RestClientException이 아니어서 따로 잡는다
            // 예외 메시지에 헤더 값이 들어 있어 예외 종류만 남기고 원인 예외도 담지 않는다
            log.warn("항공사 응답 오류: interfaceName={}, resultCode={}, cause={}",
                    INTERFACE_NAME, resultCode, e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.AIRLINE_ERROR);
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

    // 통신 실패는 원인 예외까지 남긴다. 응답을 받은 뒤의 오류는 본문에 개인정보가 있을 수 있어 예외 종류만 남긴다
    private void logFailure(String resultCode, RestClientException e) {
        if (e instanceof ResourceAccessException) {
            log.warn("항공사 통신 실패: interfaceName={}, resultCode={}", INTERFACE_NAME, resultCode, e);
            return;
        }
        String cause = NestedExceptionUtils.getMostSpecificCause(e).getClass().getSimpleName();
        log.warn("항공사 응답 오류: interfaceName={}, resultCode={}, cause={}", INTERFACE_NAME, resultCode, cause);
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
