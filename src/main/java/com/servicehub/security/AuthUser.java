package com.servicehub.security;

import com.servicehub.model.Role;
import com.servicehub.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal autenticado (UserDetails) com o id e o papel do usuário.
 * isEnabled() reflete users.is_active: usuário excluído logicamente não autentica
 * e o JWT dele deixa de valer.
 */
public class AuthUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public AuthUser(Long id, String email, String passwordHash, Role role, boolean active) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public static AuthUser from(User user) {
        return new AuthUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.isActive());
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
