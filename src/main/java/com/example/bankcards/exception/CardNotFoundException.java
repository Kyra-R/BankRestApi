package com.example.bankcards.exception;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(){
        super("Карта не найдена!");
    }
    public CardNotFoundException(String s) {
        super("Карта не найдена! " + s);
    }
}
