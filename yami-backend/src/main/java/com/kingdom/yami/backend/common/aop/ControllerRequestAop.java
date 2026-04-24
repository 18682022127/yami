package com.kingdom.yami.backend.common.aop;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ControllerRequestAop {

	@Around("within(@org.springframework.web.bind.annotation.RestController *)")
	public Object aroundRestController(ProceedingJoinPoint pjp) throws Throwable {
		return pjp.proceed();
	}

	private ResponseEntity<Map<String, Object>> illegalResponse() {
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("code", "999999");
		resp.put("message", "请求非法");
		resp.put("data", null);
		resp.put("timeStamp", String.valueOf(Instant.now().toEpochMilli()));
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resp);
	}
}
