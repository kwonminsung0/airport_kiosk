package com.example.airport_kiosk.checkin.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.airport_kiosk.checkin.dto.SeatMapResponse;
import com.example.airport_kiosk.checkin.dto.SeatResponse;
import com.example.airport_kiosk.checkin.service.SeatService;
import com.example.airport_kiosk.common.exception.BusinessException;
import com.example.airport_kiosk.common.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SeatController.class)
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeatService seatService;

    @Test
    @DisplayName("좌석 배치도를 조회하면 200과 행·열 수, 좌석별 상태를 응답한다")
    void returnsSeatMap() throws Exception {
        when(seatService.getSeatMap(1L)).thenReturn(new SeatMapResponse(1L, 10, 6, List.of(
                new SeatResponse(11L, "1A", "AVAILABLE"),
                new SeatResponse(12L, "1B", "ASSIGNED"))));

        mockMvc.perform(get("/api/checkin/flights/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightId").value(1))
                .andExpect(jsonPath("$.seatRows").value(10))
                .andExpect(jsonPath("$.seatCols").value(6))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andExpect(jsonPath("$.seats[1].seatId").value(12))
                .andExpect(jsonPath("$.seats[1].seatNo").value("1B"))
                .andExpect(jsonPath("$.seats[1].status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("좌석 응답에는 누가 앉았는지(pnr)를 담지 않는다")
    void doesNotExposePnr() throws Exception {
        when(seatService.getSeatMap(1L)).thenReturn(new SeatMapResponse(1L, 10, 6, List.of(
                new SeatResponse(12L, "1B", "ASSIGNED"))));

        mockMvc.perform(get("/api/checkin/flights/1/seats"))
                .andExpect(jsonPath("$.seats[0].pnr").doesNotExist());
    }

    @Test
    @DisplayName("항공편이 없으면 404와 FLIGHT_NOT_FOUND를 응답한다")
    void returnsNotFound() throws Exception {
        when(seatService.getSeatMap(99L)).thenThrow(new BusinessException(ErrorCode.FLIGHT_NOT_FOUND));

        mockMvc.perform(get("/api/checkin/flights/99/seats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FLIGHT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(ErrorCode.FLIGHT_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("항공편 ID가 숫자가 아니면 400을 응답하고 서비스를 호출하지 않는다")
    void rejectsNonNumericFlightId() throws Exception {
        mockMvc.perform(get("/api/checkin/flights/abc/seats"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(seatService);
    }
}
