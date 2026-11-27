package com.example.airport_kiosk.checkin.controller;

import com.example.airport_kiosk.checkin.dto.SeatMapResponse;
import com.example.airport_kiosk.checkin.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkin/flights")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{flightId}/seats")
    public SeatMapResponse getSeatMap(@PathVariable Long flightId) {
        return seatService.getSeatMap(flightId);
    }
}
