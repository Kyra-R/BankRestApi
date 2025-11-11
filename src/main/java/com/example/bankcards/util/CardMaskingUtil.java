package com.example.bankcards.util;

public class CardMaskingUtil {

    public static String getMaskedNumber(String number) {
        if (number == null || number.length() < 4) return "****";
        return "**** **** **** " + number.substring(number.length() - 4);
    }

}
