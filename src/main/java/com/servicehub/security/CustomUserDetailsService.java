package com.servicehub.security;

import com.servicehub.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Carrega o usuário pelo e-mail (normalizado em minúsculas) para o Spring Security. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(email)
                .map(AuthUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
