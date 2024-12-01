package io.github.ziy1.nexevent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
public record AuthRegisterRequestDto(
        @JsonProperty("user_id")
        @NotBlank(message = "User ID cannot be blank")
        String userId,
        @JsonProperty("password")
        @NotBlank(message = "Password cannot be blank")
        String password,
        @JsonProperty("first_name")
        @NotBlank(message = "First name cannot be blank")
        String firstName,
        @JsonProperty("last_name")
        @NotBlank(message = "Last name cannot be blank")
        String lastName) {
}
