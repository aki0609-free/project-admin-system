package com.project.backend.app.security.jwt.exception;

import com.project.backend.common.error.enums.ErrorCode;
import com.project.backend.common.exception.base.BusinessException;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.AUTH_TOKEN_EXPIRED);
    }
}
