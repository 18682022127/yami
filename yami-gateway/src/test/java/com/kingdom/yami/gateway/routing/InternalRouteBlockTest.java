package com.kingdom.yami.gateway.routing;

import com.kingdom.yami.gateway.config.InternalRouteBlockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(InternalRouteBlockConfig.class)
class InternalRouteBlockTest {

    private final MockMvc mockMvc;

    @Autowired
    InternalRouteBlockTest(WebApplicationContext wac) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void getInternalAuthEncKeyBySessionId_returns404() throws Exception {
        mockMvc.perform(get("/ymb/internal/auth/getEncKeyBySessionId"))
                .andExpect(status().isNotFound());
    }
}
