package io.github.ziy1.nexevent.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRegisterRequestDto(
    @NotBlank(message = "User ID cannot be blank") String userId,
    @NotBlank(message = "Password cannot be blank") String password,
    @NotBlank(message = "First name cannot be blank") String firstName,
    @NotBlank(message = "Last name cannot be blank") String lastName) {}
