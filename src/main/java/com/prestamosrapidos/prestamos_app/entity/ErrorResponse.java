package com.prestamosrapidos.prestamos_app.entity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private String message;
    private LocalDateTime timestamp;
}
