package com.waiitz.suji_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotBlank
    @Size(max = 2000)
    private String message;
}
