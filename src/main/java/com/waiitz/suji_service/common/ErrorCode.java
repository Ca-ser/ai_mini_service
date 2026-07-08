package com.waiitz.suji_service.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400001, "Bad Request"),
    UNAUTHORIZED(401001, "Unauthorized"),
    FORBIDDEN(403001, "Forbidden"),
    NOT_FOUND(404001, "Not Found"),
    CONFLICT(409001, "Conflict"),
    RATE_LIMITED(429001, "Rate Limited"),
    SYSTEM_ERROR(500001, "System Error"),
    AI_SERVICE_ERROR(500101, "AI Service Error"),
    WECHAT_API_ERROR(500201, "WeChat API Error"),
    ASYNC_TASK_ERROR(500301, "Async Task Error");

    private final int code;
    private final String message;

}
