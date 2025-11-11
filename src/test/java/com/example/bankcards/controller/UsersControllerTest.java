package com.example.bankcards.controller;


import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.security.JWTFilter;
import com.example.bankcards.security.JWTUtil;

import com.example.bankcards.service.UserService;
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

import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsersController.class)
@Import(GlobalExceptionHandler.class)
public class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JWTFilter jwtFilter;

    @MockBean
    private JWTUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllUsers_success() throws Exception {
        UserDTO user = new UserDTO();
        Page<UserDTO> page = new PageImpl<>(List.of(user));
        when(userService.getAllUsers(0, 6)).thenReturn(page);

        mockMvc.perform(get("/bank/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getUser_success() throws Exception {
        UserDTO user = new UserDTO();
        when(userService.getUser("admin")).thenReturn(user);

        mockMvc.perform(get("/bank/users/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_success() throws Exception {
        UserDTO dto = new UserDTO();
        when(userService.createUser(any(UserDTO.class))).thenReturn(new UserDTO());

        mockMvc.perform(post("/bank/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUser_success() throws Exception {
        UserDTO dto = new UserDTO();
        doNothing().when(userService).updateUser(eq("admin"), any(UserDTO.class));

        mockMvc.perform(put("/bank/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deleteUser("admin");

        mockMvc.perform(delete("/bank/users/admin")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
