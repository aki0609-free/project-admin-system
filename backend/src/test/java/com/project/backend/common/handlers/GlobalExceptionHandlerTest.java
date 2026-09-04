package com.project.backend.common.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationDeniedException;

import com.project.backend.common.error.dto.ErrorResponse;
import com.project.backend.common.error.enums.ErrorCode;

class GlobalExceptionHandlerTest {

    @Test
    void handleAccessDenied_shouldReturnForbidden() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/users"
        );

        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(
                        new AuthorizationDeniedException("Access Denied"),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED.getCode());
        assertThat(response.getBody().getMessage())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED.getMessage());
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequestAndBusinessMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/system/excel-book-masters/1/spreadsheet-template"
        );

        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException(
                                "コード生成台帳にはテンプレートを登録できません。"
                        ),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(response.getBody().getMessage())
                .contains("コード生成台帳");
    }
}
