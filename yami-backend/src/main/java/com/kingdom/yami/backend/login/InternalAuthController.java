package com.kingdom.yami.backend.login;

import com.kingdom.yami.common.web.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

	private final AuthService authService;

	public InternalAuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/getEncKeyBySessionId")
	public ApiResponse<String> getEncKeyBySessionId(@RequestBody GetEncKeyBySessionIdRequest req) {
		return ApiResponse.ok(authService.getSessionEncKeyBySessionId(req.sessionId()));
	}

	public record GetEncKeyBySessionIdRequest(String sessionId) {
	}
}
