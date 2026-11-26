package com.example.airport_kiosk.airline.mock;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 가상 항공사 API. 키오스크 코드는 이 클래스를 직접 부르지 않고 HTTP로만 호출한다 */
@RestController
@RequestMapping("/mock-airline")
@RequiredArgsConstructor
public class MockAirlineController {

    private final MockReservationMapper mockReservationMapper;

    // 조회지만 성이 주소와 접속 기록에 남지 않도록 POST 본문으로 받는다
    @PostMapping("/reservations/search")
    public ResponseEntity<MockReservationResponse> findReservation(@Valid @RequestBody MockReservationRequest request) {
        MockReservationResponse reservation =
                mockReservationMapper.findReservation(request.getPnr(), request.getLastName());
        if (reservation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reservation);
    }
}
