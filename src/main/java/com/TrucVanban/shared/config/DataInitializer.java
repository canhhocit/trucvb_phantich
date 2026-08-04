package com.TrucVanban.shared.config;

import com.TrucVanban.auth.entity.Role;
import com.TrucVanban.auth.entity.User;
import com.TrucVanban.auth.repository.RoleRepository;
import com.TrucVanban.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            Optional<Role> roleOpt = roleRepository.findByCode("ROLE_SUPER_ADMIN");
            
            if (roleOpt.isPresent()) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("123456"))
                        .email("admin@example.com")
                        .fullName("System Admin")
                        .roles(Set.of(roleOpt.get()))
                        .build();
                
                userRepository.save(admin);
                log.info("Initialized default admin user");
            } else {
                log.warn("ROLE_SUPER_ADMIN not found, cannot initialize admin user");
            }
        }
    }
}
