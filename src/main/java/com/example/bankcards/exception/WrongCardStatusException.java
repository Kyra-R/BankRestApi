package com.example.bankcards.exception;

public class WrongCardStatusException extends RuntimeException {
    public WrongCardStatusException(String s) {
        super("Некорректный статус карты: " + s);
    }
}