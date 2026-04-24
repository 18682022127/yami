package com.kingdom.yami.backend.login;

import com.kingdom.yami.common.web.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ApiResponse<AuthService.LoginResponse> login(@RequestBody LoginRequest req) {
		return ApiResponse.ok(authService.loginWithPhoneCode(req.phone(), req.code()));
	}

	@PostMapping("/checkToken")
	public boolean checkToken(@RequestBody CheckTokenRequest req) {
		return authService.checkToken(req.token());
	}

	public record LoginRequest(String phone, String code) {
	}
	public record CheckTokenRequest(String token) {
	}
}
