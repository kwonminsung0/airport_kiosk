package com.example.airport_kiosk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("첫 주소로 접속하면 시작 화면을 보여 준다")
    void showsIndex() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    }

    @ParameterizedTest
    @DisplayName("화면에 필요한 파일을 제공한다")
    @ValueSource(strings = {"/index.html", "/checkin/lookup.html", "/js/lookup.js", "/css/style.css"})
    void servesStaticFiles(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }
}
