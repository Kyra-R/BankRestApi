package com.example.bankcards.controller;


import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.security.JWTUtil;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bank/auth")
@RequiredArgsConstructor
public class AuthorizationController {
    private final UserService userService;

    @Operation(summary = "Регистрация нового пользователя", description = "Позволяет зарегистрировать пользователя с логином и паролем")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная регистрация"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные регистрации / пользователь уже существует"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid AuthDTO authDTO) {
        userService.register(authDTO);
        return ResponseEntity.ok(Map.of("message", "Успешная регистрация"));
    }


    @Operation(summary = "Авторизация пользователя", description = "Позволяет получить JWT токен по логину и паролю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен успешно выдан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"jwt\": \"eyJhbGciOiJIUzI1NiIsInR5cCI...\"}"))),
            @ApiResponse(responseCode = "400", description = "Неверный логин или пароль"),
            @ApiResponse(responseCode = "403", description = "Неавторизованный доступ")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDTO authDTO) {
            String token = userService.login(authDTO);;
            return ResponseEntity.ok(Map.of("jwt", token));
    }
}
