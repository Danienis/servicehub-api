package com.servicehub.service;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Sanitização de entradas de texto livre (Aula 08): normaliza Unicode (NFKC), remove caracteres de
 * controle, faz trim e escapa HTML/XML (&, <, >, ", ') para evitar XSS armazenado.
 *
 * Usa escapeXml11 (e não escapeHtml4) para NÃO transformar acentos do português em entidades
 * (ex.: "ção" continua "ção"). Queries ao banco usam apenas parâmetros (JPA/Criteria), nunca concatenação.
 */
@Component
public class InputSanitizer {

    /** Sanitiza texto sem limite de tamanho (colunas TEXT). Retorna null para null/vazio. */
    public String clean(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "")
                .strip();
        if (normalized.isEmpty()) {
            return null;
        }
        return StringEscapeUtils.escapeXml11(normalized);
    }

    /** Sanitiza texto de coluna VARCHAR(max): o escape pode aumentar o tamanho, então revalida. */
    public String clean(String value, int maxLength) {
        String cleaned = clean(value);
        if (cleaned != null && cleaned.length() > maxLength) {
            throw new IllegalArgumentException(
                    "O texto excede " + maxLength + " caracteres após a sanitização (caracteres especiais são escapados)");
        }
        return cleaned;
    }

    /** E-mail: trim + minúsculas (a coluna tem CHECK email = LOWER(email)). */
    public String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
