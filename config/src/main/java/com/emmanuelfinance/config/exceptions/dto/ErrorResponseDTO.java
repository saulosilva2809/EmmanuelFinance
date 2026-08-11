package com.emmanuelfinance.config.exceptions.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO (
        LocalDateTime timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<ValidationErrorField> errors
) {
    public ErrorResponseDTO(LocalDateTime timestamp, Integer status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }

    public record ValidationErrorField(String field, String message) {}
}
