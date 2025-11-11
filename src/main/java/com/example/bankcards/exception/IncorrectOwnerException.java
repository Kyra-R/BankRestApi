package com.example.bankcards.exception;

public class IncorrectOwnerException extends RuntimeException {
    public IncorrectOwnerException(String username) {
        super("Попытка взаимодействия с картой, не принадлежащей " + username);
    }
}
