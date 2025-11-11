package com.example.bankcards.exception;


public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("Пользователь не найден: " + username);
    }
}