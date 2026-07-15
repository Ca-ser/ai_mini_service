package com.waiitz.suji_service.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ErrorCode.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ErrorCode.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    public static BizException rateLimited(String message) {
        return new BizException(ErrorCode.RATE_LIMITED, message);
    }

}
