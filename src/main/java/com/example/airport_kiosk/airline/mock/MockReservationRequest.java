package com.example.airport_kiosk.airline.mock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 가상 항공사 API의 예약 조회 요청. 성은 개인정보이므로 주소가 아니라 본문으로 받는다 */
@Getter
public class MockReservationRequest {

    @NotBlank
    private final String pnr;

    @NotBlank
    private final String lastName;

    @JsonCreator
    public MockReservationRequest(@JsonProperty("pnr") String pnr, @JsonProperty("lastName") String lastName) {
        this.pnr = pnr;
        this.lastName = lastName;
    }
}
