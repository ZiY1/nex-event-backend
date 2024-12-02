package io.github.ziy1.nexevent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthLoginResponseDto(
        String accessToken) {
}