package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardsRepository;
import com.example.bankcards.repository.RolesRepository;
import com.example.bankcards.repository.UsersRepository;
import com.example.bankcards.security.JWTUtil;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.EncryptionUtil;
import com.example.bankcards.util.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UsersRepository usersRepository;

    private final RolesRepository rolesRepository;

    private final UserMapper userMapper;


    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JWTUtil jwtUtil;

    @Autowired
    public UserService(UsersRepository usersRepository, RolesRepository rolesRepository,
                       UserMapper userMapper, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JWTUtil jwtUtil){
        this.usersRepository = usersRepository;
        this.rolesRepository = rolesRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public UserDTO getUser(String username){
        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + username));
        return userMapper.toDTO(user);
    }

    public Page<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> usersPage = usersRepository.findAll(pageable);
        return usersPage.map(userMapper::toDTO);
    }


    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        if (usersRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            throw new RuntimeException("Отсутствует пароль");
        }

        User user = userMapper.fromDTO(userDTO);

        user.setPassword("{bcrypt}" + passwordEncoder.encode(userDTO.getPassword()));

        User saved = usersRepository.save(user);
        return userMapper.toDTO(saved);
    }

    @Transactional
    public void updateUser(String username, UserDTO userDTO) {
        User existingUser =  usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (userDTO.getUsername() != null && !userDTO.getUsername().isBlank()) {
            existingUser.setUsername(userDTO.getUsername());
        }

        if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
            existingUser.setPassword("{bcrypt}" + passwordEncoder.encode(userDTO.getPassword()));
        }

        if (userDTO.getRole() != null && !userDTO.getRole().isBlank()) {
            Role role = rolesRepository.findByName(userDTO.getRole())
                    .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + userDTO.getRole()));
            existingUser.setRole(role);
        }

        usersRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(String username) {
        usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        usersRepository.deleteByUsername(username);
    }

    public String login(AuthDTO authDTO) {

            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authDTO.getUsername(), authDTO.getPassword()
                    )
            );

            return jwtUtil.generateToken(authDTO.getUsername());

    }

    public void register(AuthDTO authDTO) {

        if (usersRepository.findByUsername(authDTO.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        if (authDTO.getPassword() == null || authDTO.getPassword().isBlank()) {
            throw new RuntimeException("Отсутствует пароль");
        }

        User user = userMapper.fromDTO(authDTO);

        user.setPassword("{bcrypt}" + passwordEncoder.encode(authDTO.getPassword()));

        usersRepository.save(user);

    }

}
