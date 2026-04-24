package com.kingdom.yami.backend.common.handler;

import com.kingdom.yami.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    ApiResponse<?> handlerException(Exception e) {
        log.error(e.getMessage(),e);
        return ApiResponse.fail(e.getMessage());
    }

}