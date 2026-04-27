package com.kingdom.yami.gateway.routing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class InternalRouteBlockTest {

	private final MockMvc mockMvc;

	InternalRouteBlockTest(WebApplicationContext wac) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@Test
	void internalPathShouldReturn404() throws Exception {
		mockMvc.perform(get("/ymb/internal/auth/getEncKeyBySessionId"))
			.andExpect(status().isNotFound());
	}
}
