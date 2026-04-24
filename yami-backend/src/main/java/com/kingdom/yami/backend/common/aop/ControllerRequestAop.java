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

}
