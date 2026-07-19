package com.project.backend.app.base.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageResponse {
    private String message;
}
