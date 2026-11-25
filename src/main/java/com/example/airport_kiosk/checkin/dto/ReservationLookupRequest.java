package com.example.airport_kiosk.checkin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;
import lombok.Getter;

/** 예약 조회 요청. 생성자에서 입력값을 정리한 뒤 형식을 검증한다 */
@Getter
public class ReservationLookupRequest {

    private static final String PNR_MESSAGE = "예약번호는 영문·숫자 6자리로 입력해 주세요.";
    private static final String LAST_NAME_MESSAGE = "성은 영문으로 입력해 주세요.";

    @NotBlank(message = PNR_MESSAGE)
    @Pattern(regexp = "[A-Z0-9]{6}", message = PNR_MESSAGE)
    private final String pnr;

    @NotBlank(message = LAST_NAME_MESSAGE)
    @Pattern(regexp = "[A-Za-z][A-Za-z '-]{0,49}", message = LAST_NAME_MESSAGE)
    private final String lastName;

    // null은 그대로 두어 @NotBlank가 400을 내게 한다
    @JsonCreator
    public ReservationLookupRequest(@JsonProperty("pnr") String pnr, @JsonProperty("lastName") String lastName) {
        this.pnr = pnr == null ? null : pnr.trim().toUpperCase(Locale.ROOT);
        this.lastName = lastName == null ? null : lastName.trim();
    }
}
