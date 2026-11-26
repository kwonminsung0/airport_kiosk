package com.example.airport_kiosk.airline.mock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MockAirlineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("예약번호와 성이 맞으면 예약과 항공편 정보를 돌려준다")
    void returnsReservation() throws Exception {
        search("{\"pnr\":\"ABC123\",\"lastName\":\"KIM\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("ABC123"))
                .andExpect(jsonPath("$.lastName").value("KIM"))
                .andExpect(jsonPath("$.firstName").value("MINSU"))
                .andExpect(jsonPath("$.baggageAllowanceKg").value(23))
                .andExpect(jsonPath("$.flightId").isNumber())
                .andExpect(jsonPath("$.flightNo").value("ZZ101"))
                .andExpect(jsonPath("$.depAirport").value("ICN"))
                .andExpect(jsonPath("$.arrAirport").value("NRT"))
                .andExpect(jsonPath("$.depTime").isString())
                .andExpect(jsonPath("$.gate").value("12"));
    }

    @Test
    @DisplayName("성은 대소문자를 구분하지 않는다")
    void ignoresLastNameCase() throws Exception {
        search("{\"pnr\":\"ABC123\",\"lastName\":\"kim\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("ABC123"));
    }

    @Test
    @DisplayName("없는 예약번호면 404를 돌려준다")
    void returns404WhenPnrNotFound() throws Exception {
        search("{\"pnr\":\"ZZZ999\",\"lastName\":\"KIM\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("예약번호는 맞지만 성이 다르면 404를 돌려준다")
    void returns404WhenLastNameDiffers() throws Exception {
        search("{\"pnr\":\"ABC123\",\"lastName\":\"LEE\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("성을 보내지 않으면 400을 돌려준다")
    void returns400WhenLastNameMissing() throws Exception {
        search("{\"pnr\":\"ABC123\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예약번호를 보내지 않으면 400을 돌려준다")
    void returns400WhenPnrMissing() throws Exception {
        search("{\"lastName\":\"KIM\"}")
                .andExpect(status().isBadRequest());
    }

    private ResultActions search(String body) throws Exception {
        return mockMvc.perform(post("/mock-airline/reservations/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
