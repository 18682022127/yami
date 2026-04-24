package com.kingdom.yami.backend.index;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/index")
public class IndexController {

	@PostMapping("/test")
	public Map<String, Object> test(@RequestBody Map<String, Object> body) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("xxx", "yyy");
		return response;
	}
}
