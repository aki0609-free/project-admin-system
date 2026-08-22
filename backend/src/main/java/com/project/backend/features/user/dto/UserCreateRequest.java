package com.project.backend.features.user.dto;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class UserCreateRequest {
    @NotBlank(message = "ユーザー名は必須です。")
    @Size(max = 100, message = "ユーザー名は100文字以内で入力してください。")
    private String username;

    @NotBlank(message = "パスワードは必須です。")
    private String password;

    @NotNull(message = "利用状態は必須です。")
    private Boolean enabled;

    @NotEmpty(message = "ロールは1件以上必要です。")
    private Set<String> roles;
}
