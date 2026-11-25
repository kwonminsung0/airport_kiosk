package com.example.airport_kiosk.checkin.service.impl;

import com.example.airport_kiosk.airline.client.AirlineClient;
import com.example.airport_kiosk.airline.client.AirlineReservation;
import com.example.airport_kiosk.checkin.dto.ReservationLookupResponse;
import com.example.airport_kiosk.checkin.mapper.CheckinMapper;
import com.example.airport_kiosk.checkin.service.CheckinService;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CheckinServiceImpl implements CheckinService {

    private final AirlineClient airlineClient;
    private final CheckinMapper checkinMapper;
    private final Clock clock;
    private final Duration openBefore;
    private final Duration closeBefore;

    public CheckinServiceImpl(
            AirlineClient airlineClient,
            CheckinMapper checkinMapper,
            Clock clock,
            @Value("${checkin.open-before}") Duration openBefore,
            @Value("${checkin.close-before}") Duration closeBefore) {
        this.airlineClient = airlineClient;
        this.checkinMapper = checkinMapper;
        this.clock = clock;
        this.openBefore = openBefore;
        this.closeBefore = closeBefore;
    }

    // 항공사에서 성까지 확인한 뒤에 체크인 상태를 알려 준다 (예약번호만으로 상태가 드러나지 않게)
    @Override
    public ReservationLookupResponse lookupReservation(String pnr, String lastName) {
        AirlineReservation reservation = airlineClient.findReservation(pnr, lastName);
        if (checkinMapper.countByPnr(pnr) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_CHECKED_IN);
        }
        validateCheckinTime(reservation.getDepTime());
        return ReservationLookupResponse.from(reservation);
    }

    // 시작 시각과 마감 시각 정각은 체크인할 수 있다
    private void validateCheckinTime(LocalDateTime depTime) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime opensAt = depTime.minus(openBefore);
        LocalDateTime closesAt = depTime.minus(closeBefore);
        if (now.isBefore(opensAt) || now.isAfter(closesAt)) {
            throw new BusinessException(ErrorCode.CHECKIN_NOT_AVAILABLE, checkinTimeMessage());
        }
    }

    // 설정값으로 문구를 만들어, 설정을 바꾸면 안내도 함께 바뀌게 한다
    private String checkinTimeMessage() {
        return "체크인은 출발 " + toKorean(openBefore) + " 전부터 " + toKorean(closeBefore) + " 전까지 할 수 있습니다.";
    }

    // 24h → "24시간", 90m → "1시간 30분", 45m → "45분"
    private String toKorean(Duration duration) {
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        if (hours == 0) {
            return minutes + "분";
        }
        if (minutes == 0) {
            return hours + "시간";
        }
        return hours + "시간 " + minutes + "분";
    }
}
