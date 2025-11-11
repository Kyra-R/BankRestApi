package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@Schema(description = "DTO для аутентификации пользователя (логин и пароль)")
public class AuthDTO {
    @Schema(description = "Имя пользователя", example = "testuser", required = true)
    private String username;

    @Schema(description = "Пароль пользователя", example = "qwerty", required = true)
    private String password;
}
