package com.servicehub.model;

/**
 * Categorias dos serviços. Deve ser idêntico ao CHECK chk_services_category do banco
 * (nova categoria = nova migração Flyway).
 */
public enum ServiceCategory {
    LIMPEZA,
    DIARISTA,
    ENCANAMENTO,
    ELETRICA,
    JARDINAGEM,
    PINTURA,
    MARCENARIA,
    PEDREIRO,
    DEDETIZACAO,
    AR_CONDICIONADO,
    CHAVEIRO,
    MONTAGEM_MOVEIS,
    MUDANCA,
    SUPORTE_TECNICO,
    MANUTENCAO_COMPUTADORES,
    DESENVOLVIMENTO_SOFTWARE,
    REDES_E_INFRAESTRUTURA,
    SEGURANCA_DIGITAL,
    RECUPERACAO_DE_DADOS,
    CONSULTORIA_TI,
    OUTROS
}
