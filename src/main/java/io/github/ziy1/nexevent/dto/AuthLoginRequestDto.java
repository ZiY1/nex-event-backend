package io.github.ziy1.nexevent.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequestDto(
    @NotBlank(message = "User ID cannot be blank") String userId,
    @NotBlank(message = "Password cannot be blank") String password) {}
