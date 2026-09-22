package com.servicehub.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Estrutura padronizada de resposta de erro da API")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Schema(description = "Momento em que o erro ocorreu")
    private LocalDateTime timestamp;

    @Schema(description = "Código de status HTTP", example = "404")
    private int status;

    @Schema(description = "Categoria do erro", example = "Not Found")
    private String error;

    @Schema(description = "Mensagem descritiva do erro", example = "Usuário não encontrado: 99")
    private String message;

    @Schema(description = "Caminho da requisição que originou o erro", example = "/api/v1/users/99")
    private String path;

    @Schema(description = "Lista de erros de validação de campos específicos, quando aplicável")
    private List<String> details;

    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ErrorResponse(int status, String error, String message, String path, List<String> details) {
        this(status, error, message, path);
        this.details = details;
    }
}
