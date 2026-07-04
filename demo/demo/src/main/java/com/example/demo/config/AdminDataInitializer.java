package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.enums.PlatformRole;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {

    @Bean
    CommandLineRunner seedAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:admin@aistudio.com}") String adminEmail,
            @Value("${app.admin.password:Admin@123}") String adminPassword,
            @Value("${app.admin.name:Platform Admin}") String adminName) {
        return args -> {
            String email = adminEmail.trim().toLowerCase();
            userRepository.findByEmail(email).ifPresentOrElse(
                    user -> {
                        if (user.getPlatformRole() != PlatformRole.ADMIN) {
                            user.setPlatformRole(PlatformRole.ADMIN);
                            userRepository.save(user);
                        }
                    },
                    () -> {
                        User admin = new User();
                        admin.setEmail(email);
                        admin.setPassword(passwordEncoder.encode(adminPassword));
                        admin.setFullName(adminName);
                        admin.setPlatformRole(PlatformRole.ADMIN);
                        userRepository.save(admin);
                    }
            );
        };
    }
}
