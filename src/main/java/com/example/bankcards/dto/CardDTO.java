package com.example.bankcards.dto;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO для отображения информации о банковской карте")
public class CardDTO {
    @Schema(description = "Номер карты (замаскирован для обычного пользователя, виден админу)",
            example = "**** **** **** 1234")
    public String maskedNumber;

    @Schema(description = "Имя владельца карты", example = "testuser")
    public String ownerUsername;

    @Schema(description = "Месяц окончания срока действия карты", example = "12")
    public int expirationMonth;

    @Schema(description = "Год окончания срока действия карты", example = "2026")
    public int expirationYear;

    @Schema(description = "Статус карты", example = "ACTIVE")
    public CardStatus status;

    @Schema(description = "Баланс карты", example = "1500.50")
    public BigDecimal balance;
}
