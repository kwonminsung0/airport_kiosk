package com.example.airport_kiosk.airline.mock;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 가상 항공사 API. 키오스크 코드는 이 클래스를 직접 부르지 않고 HTTP로만 호출한다 */
@RestController
@RequestMapping("/mock-airline")
@RequiredArgsConstructor
public class MockAirlineController {

    private final MockReservationMapper mockReservationMapper;

    @GetMapping("/reservations")
    public ResponseEntity<MockReservationResponse> findReservation(
            @RequestParam("pnr") String pnr, @RequestParam("lastName") String lastName) {
        MockReservationResponse reservation = mockReservationMapper.findReservation(pnr, lastName);
        if (reservation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reservation);
    }
}
