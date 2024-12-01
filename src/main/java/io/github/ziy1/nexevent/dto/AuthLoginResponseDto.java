package io.github.ziy1.nexevent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthLoginResponseDto(
        @JsonProperty("access_token")
        String accessToken) {
}