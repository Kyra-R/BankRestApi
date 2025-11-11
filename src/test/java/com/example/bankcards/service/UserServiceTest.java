package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.RolesRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.security.JWTUtil;
import com.example.bankcards.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JWTUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("admin");
        user.setPassword("admin");

        userDTO = new UserDTO("admin", "admin", "ADMIN");
    }

    @Test
    void getUser_existingUser_returnsDTO() {
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.getUser("admin");

        assertEquals("admin", result.getUsername());
        verify(usersRepository).findByUsername("admin");
    }

    @Test
    void getUser_nonExistingUser_throwsException() {
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.getUser("admin"));
    }

    @Test
    void getAllUsers_returnsPageOfUserDTOs() {
        User user = new User();
        Page<User> page = new PageImpl<>(List.of(user));
        when(usersRepository.findAll(any(Pageable.class))).thenReturn(page);

        UserDTO userDTO = new UserDTO();
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        Page<UserDTO> result = userService.getAllUsers(0, 6);

        assertEquals(1, result.getContent().size());
        assertSame(userDTO, result.getContent().get(0));
        verify(usersRepository).findAll(any(Pageable.class));
    }



    @Test
    void updateUser_validData_updatesUser() {
        Role role = new Role();
        role.setName("ADMIN");

        UserDTO updatedDTO = new UserDTO("adminUpdated", "newPass", "ADMIN");

        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(rolesRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedPass");

        userService.updateUser("admin", updatedDTO);

        assertEquals("adminUpdated", user.getUsername());
        assertEquals("{bcrypt}encodedPass", user.getPassword());
        assertEquals(role, user.getRole());
        verify(usersRepository).save(user);
    }

    @Test
    void createUser_validUser_savesUser() {
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userMapper.fromDTO(userDTO)).thenReturn(user);
        when(passwordEncoder.encode("admin")).thenReturn("encoded");
        when(usersRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.createUser(userDTO);

        assertEquals("admin", result.getUsername());
        verify(usersRepository).save(user);
    }

    @Test
    void createUser_existingUser_throwsException() {
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(userDTO));
    }

    @Test
    void deleteUser_existingUser_deletes() {
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        userService.deleteUser("admin");

        verify(usersRepository).deleteByUsername("admin");
    }

    @Test
    void deleteUser_userNotFound_throwsException() {
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.deleteUser("admin"));
    }

    @Test
    void register_validUser_savesUser() {
        AuthDTO authDTO = new AuthDTO("newUser", "pass");
        User newUser = new User();

        when(usersRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userMapper.fromDTO(authDTO)).thenReturn(newUser);
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(usersRepository.save(newUser)).thenReturn(newUser);

        userService.register(authDTO);

        assertEquals("{bcrypt}encodedPass", newUser.getPassword());
        verify(usersRepository).save(newUser);
    }

    @Test
    void register_existingUser_throwsException() {
        AuthDTO authDTO = new AuthDTO("admin", "pass");
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.register(authDTO));
    }

    @Test
    void register_noPassword_throwsException() {
        AuthDTO authDTO = new AuthDTO("user", "");

        assertThrows(RuntimeException.class,
                () -> userService.register(authDTO));
    }

    @Test
    void login_validAuth_returnsToken() {
        AuthDTO authDTO = new AuthDTO("admin", "admin");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(org.springframework.security.core.Authentication.class));
        when(jwtUtil.generateToken("admin")).thenReturn("token123");

        String token = userService.login(authDTO);

        assertEquals("token123", token);
    }
}
