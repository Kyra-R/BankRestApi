package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/bank/cards")
@RequiredArgsConstructor
public class CardsController {

    private final CardService cardService;

    @Operation(summary = "Получить все карты (админ)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список карт"),
            @ApiResponse(responseCode = "401", description = "Нет авторизации"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardDTO>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return ResponseEntity.ok(cardService.getAllCards(page, size));
    }


    @Operation(summary = "Получить карты текущего пользователя (пользователь)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список карт пользователя"),
            @ApiResponse(responseCode = "403", description = "Не авторизован")
    })
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardDTO>> getUserCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(cardService.getUserCards(authentication.getName(), page, size));
    }


    @Operation(summary = "Проверить баланс карты (пользователь)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс карты"),
            @ApiResponse(responseCode = "400", description = "Карта не найдена или неверные данные карты"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @GetMapping("/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BigDecimal> checkCardBalance(
            @Parameter(description = "Номер карты без пробелов", example = "1111222233334444")
            @RequestParam() String number
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(cardService.checkCardBalance(authentication.getName(), number));
    }


    @Operation(summary = "Запрос на блокировку карты (пользователь)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запрос отправлен"),
            @ApiResponse(responseCode = "403", description = "Попытка блокировки чужой карты"),
            @ApiResponse(responseCode = "400", description = "Карта не найдена")
    })
    @PostMapping("/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> requestBlockCard(
            @Parameter(description = "Номер карты без пробелов", example = "1111222233334444")
            @RequestParam String number
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        cardService.requestBlockCard(authentication.getName(), number);
        return ResponseEntity.ok(Map.of("message", "Запрос на блокировку отправлен"));
    }

    @Operation(summary = "Изменить статус карты (админ)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус обновлен"),
            @ApiResponse(responseCode = "400", description = "Карта не найдена или неверные данные"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @PostMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeStatus(
            @Parameter(description = "Номер карты без пробелов", example = "1111222233334444")
            @RequestParam String number,
            @RequestParam String status
    ) {
        cardService.changeCardStatus(number, status);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Проверить запросы на блокировку (админ)")
    @GetMapping("/requests/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> checkBlockingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        Page<CardDTO> result = cardService.checkBlockingRequests(page, size);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Перевод между счетами (пользователь)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Деньги переведены"),
            @ApiResponse(responseCode = "403", description = "Попытка доступа к чужой карте"),
            @ApiResponse(responseCode = "403", description = "Карта или карты заблокированы или ожидают блокировки"),
            @ApiResponse(responseCode = "400", description = "Карта не найдена")
    })
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> transferBetweenCards(
            @Parameter(description = "Номер карты без пробелов", example = "1111222233334444")
            @RequestParam String fromCard,
            @Parameter(description = "Номер карты без пробелов", example = "1111222233334444")
            @RequestParam String toCard,
            @RequestParam BigDecimal amount
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        cardService.transfer(authentication.getName(), fromCard, toCard, amount);
        return ResponseEntity.ok(Map.of("message", "Перевод выполнен успешно"));
    }


    @Operation(summary = "Создать новую карту (админ)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта создана"),
            @ApiResponse(responseCode = "400", description = "Неверные данные"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDTO> createCard(
            @Parameter(description = "Номер карты без маски и пробелов", example = "1111222233334444")
            @RequestBody CardDTO cardDTO) {
        CardDTO createdCard = cardService.createCard(cardDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @Operation(summary = "Обновить карту (админ)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Данные обновлены"),
            @ApiResponse(responseCode = "400", description = "Карта не найдена или неверные данные"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @PutMapping("/{number}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDTO> updateCard(
            @Parameter(description = "Номер карты без пробелов", example = "1111222233334444")
            @PathVariable String number,
            @Parameter(description = "Номер карты без маски и пробелов", example = "1111222233334444")
            @RequestBody CardDTO cardDTO) {
        CardDTO updatedCard = cardService.updateCard(number, cardDTO);
        return ResponseEntity.ok(updatedCard);
    }

    @Operation(summary = "Удалить карту (админ)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта удалена"),
            @ApiResponse(responseCode = "400", description = "Карта не найдена или неверные данные"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @DeleteMapping("/{number}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCard(@Parameter(description = "Номер карты без пробелов", example = "1111222233334444")
                                            @PathVariable String number) {
        cardService.deleteCard(number);
        return ResponseEntity.noContent().build();
    }
}
