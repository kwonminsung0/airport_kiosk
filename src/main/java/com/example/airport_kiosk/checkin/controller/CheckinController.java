package com.example.airport_kiosk.checkin.controller;

import com.example.airport_kiosk.checkin.dto.ReservationLookupRequest;
import com.example.airport_kiosk.checkin.dto.ReservationLookupResponse;
import com.example.airport_kiosk.checkin.service.CheckinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/reservations/lookup")
    public ReservationLookupResponse lookupReservation(@Valid @RequestBody ReservationLookupRequest request) {
        return checkinService.lookupReservation(request.getPnr(), request.getLastName());
    }
}
