package com.kingdom.yami.common.web;


public record ApiResponse<T>(
    String code,
    String message,
    long timeStamp,
    T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("0", "", System.currentTimeMillis(), data);
    }

    public static <T> ApiResponse<T> fail(T data,String message) {
        return new ApiResponse<>("1", message, System.currentTimeMillis(), data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>("1", message, System.currentTimeMillis(),null);
    }

    public static <T> ApiResponse<T> fail(String code,String message) {
        return new ApiResponse<>(code, message, System.currentTimeMillis(),null);
    }
}
