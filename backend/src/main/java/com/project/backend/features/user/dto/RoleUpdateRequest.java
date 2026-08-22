package com.project.backend.features.user.dto;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class RoleUpdateRequest {
    @NotBlank(message = "ロール名は必須です。")
    @Size(max = 100, message = "ロール名は100文字以内で入力してください。")
    private String name;

    @NotEmpty(message = "権限は1件以上必要です。")
    private Set<Long> permissionIds;
}
