package com.example.bankcards.util;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final RolesRepository rolesRepository;


    @Autowired
    public UserMapper(RolesRepository rolesRepository) {
        this.rolesRepository = rolesRepository;
    }

    public UserDTO toDTO(User user){
        return new UserDTO(
                user.getUsername(),
                user.getPassword(),
                user.getRole().getName()
        );
    }

    public User fromDTO(AuthDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        Role role =  rolesRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalArgumentException("Роль USER отсутствует"));
        user.setRole(role);
        return user;
    }

    public User fromDTO(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());

        Role role;

        if(user.getRole() == null){
            role =  rolesRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalArgumentException("Роль USER отсутствует: " + dto.getRole()));
        } else {
            role = rolesRepository.findByName(dto.getRole())
                    .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + dto.getRole()));
        }

        user.setRole(role);
        return user;
    }
}
