package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO для передачи данных пользователя")
public class UserDTO {
    @Schema(description = "Имя пользователя", example = "testuser", required = true)
    private String username;

    @Schema(description = "Пароль пользователя", example = "qwerty", required = true)
    private String password;

    @Schema(description = "Роль пользователя", example = "ROLE_USER", required = true)
    private String role;
}
