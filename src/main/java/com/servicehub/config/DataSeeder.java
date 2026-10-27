package com.servicehub.config;

import com.servicehub.model.Role;
import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Cria o primeiro administrador quando servicehub.admin.email e servicehub.admin.password estão definidos
 * e o e-mail ainda não existe. No perfil H2 (demo) há um padrão; no perfil postgres vem das variáveis
 * ADMIN_EMAIL / ADMIN_PASSWORD (se vazias, nada é criado). A senha é salva com BCrypt.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${servicehub.admin.email:}") String adminEmail,
                      @Value("${servicehub.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        String email = adminEmail.strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setFullName("Administrador ServiceHub");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Administrador inicial criado: {}", email);
    }
}
