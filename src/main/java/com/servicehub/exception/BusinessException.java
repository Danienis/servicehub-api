package com.servicehub.exception;

/**
 * Exceção para violações de regras de negócio (ex: e-mail duplicado),
 * mapeada para HTTP 409 Conflict pelo GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
