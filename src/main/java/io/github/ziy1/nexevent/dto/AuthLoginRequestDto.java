package io.github.ziy1.nexevent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequestDto(
        @JsonProperty("user_id")
        @NotBlank(message = "User ID cannot be blank")
        String userId,
        @JsonProperty("password")
        @NotBlank(message = "Password cannot be blank")
        String password) {
}