package com.servicehub.model;

/**
 * Papel único de cada usuário (coluna users.role). No Spring Security vira a authority ROLE_{nome}.
 */
public enum Role {
    ADMIN,
    PROVIDER,
    CLIENT
}
