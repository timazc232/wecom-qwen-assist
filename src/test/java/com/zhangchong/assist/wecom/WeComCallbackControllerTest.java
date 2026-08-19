package com.zhangchong.assist.wecom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class WeComCallbackControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void verifyEcho() throws Exception {
        mvc.perform(get("/wecom/callback").param("echostr", "ping-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("ping-1"));
    }

    @Test
    public void inboundCreatesSession() throws Exception {
        mvc.perform(post("/wecom/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"FromUserName\":\"wx-demo\",\"Content\":\"门禁刷不开\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.userId").value("wx-demo"));
    }
}
