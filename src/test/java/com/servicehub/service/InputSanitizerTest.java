package com.servicehub.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputSanitizerTest {

    private final InputSanitizer sanitizer = new InputSanitizer();

    @Test
    @DisplayName("deve escapar tags HTML para evitar XSS")
    void deveEscaparHtml() {
        assertThat(sanitizer.clean("<script>alert('x')</script>"))
                .doesNotContain("<").doesNotContain(">").contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("deve preservar acentos do português")
    void devePreservarAcentos() {
        assertThat(sanitizer.clean("  Instalação elétrica  ")).isEqualTo("Instalação elétrica");
    }

    @Test
    @DisplayName("deve retornar null para nulo ou vazio")
    void deveRetornarNull() {
        assertThat(sanitizer.clean(null)).isNull();
        assertThat(sanitizer.clean("   ")).isNull();
    }

    @Test
    @DisplayName("deve remover caracteres de controle")
    void deveRemoverControle() {
        assertThat(sanitizer.clean("a\u0000b")).isEqualTo("ab");
    }

    @Test
    @DisplayName("deve rejeitar texto que excede o tamanho após o escape")
    void deveRejeitarTextoGrande() {
        assertThatThrownBy(() -> sanitizer.clean("<<<<<", 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveNormalizarEmail() {
        assertThat(sanitizer.normalizeEmail("  Joao@HUB.com ")).isEqualTo("joao@hub.com");
    }
}
