package com.example.bankcards.controller;


import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.service.CardService;
import com.example.bankcards.security.JWTFilter;
import com.example.bankcards.security.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import java.util.List;



import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardsController.class)
@Import(GlobalExceptionHandler.class)
public class CardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private JWTFilter jwtFilter;

    @MockBean
    private JWTUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;


    // ADMIN

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllCards_success() throws Exception {
        CardDTO card = new CardDTO();
        Page<CardDTO> page = new PageImpl<>(List.of(card));
        when(cardService.getAllCards(0, 6)).thenReturn(page);

        mockMvc.perform(get("/bank/cards/all")
                        .param("page", "0")
                        .param("size", "6"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createCard_sanityCheck() throws Exception {
        CardDTO dto = new CardDTO();
        dto.setMaskedNumber("1111222233334444");
        dto.setOwnerUsername("admin");

        when(cardService.createCard(any(CardDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/bank/cards/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeStatus_success() throws Exception {
        doNothing().when(cardService).changeCardStatus(anyString(), anyString());

        mockMvc.perform(post("/bank/cards/status")
                        .param("number", "1111222233334444")
                        .param("status", "BLOCKED")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteCard_success() throws Exception {
        doNothing().when(cardService).deleteCard("1111222233334444");

        mockMvc.perform(delete("/bank/cards/1111222233334444")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void checkBlockingRequests_success() throws Exception {
        CardDTO card = new CardDTO();
        Page<CardDTO> page = new PageImpl<>(List.of(card));
        when(cardService.checkBlockingRequests(0, 6)).thenReturn(page);

        mockMvc.perform(get("/bank/cards/requests/block")
                        .param("page", "0")
                        .param("size", "6"))
                .andExpect(status().isOk());
    }

    // USER

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getUserCards_success() throws Exception {
        CardDTO card = new CardDTO();
        Page<CardDTO> page = new PageImpl<>(List.of(card));
        when(cardService.getUserCards(eq("user"), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/bank/cards")
                        .param("page", "0")
                        .param("size", "6"))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void requestBlockCard_success() throws Exception {
        doNothing().when(cardService).requestBlockCard(eq("user"), anyString());

        mockMvc.perform(post("/bank/cards/block")
                        .param("number", "1111222233334444")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void transferBetweenCards_success() throws Exception {
        doNothing().when(cardService)
                .transfer(eq("user"), anyString(), anyString(), any(BigDecimal.class));

        mockMvc.perform(post("/bank/cards/transfer")
                        .param("fromCard", "1111222233334444")
                        .param("toCard", "5555666677778888")
                        .param("amount", "500")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}