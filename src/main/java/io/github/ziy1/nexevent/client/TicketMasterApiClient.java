package io.github.ziy1.nexevent.client;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

import io.github.ziy1.nexevent.dto.TicketMasterApiResponseDto;
import io.github.ziy1.nexevent.util.IdNormalizerUtil;

@Slf4j
@Component
public class TicketMasterApiClient {
  private final WebClient webClient;

  @Value("${ticketmaster.base-url:https://app.ticketmaster.com/discovery/v2/events.json}")
  private String baseUrl;

  @Value("${ticketmaster.api-key}")
  private String apiKey;

  @Value("${ticketmaster.default-keyword:}")
  private String defaultKeyword;

  @Value("${ticketmaster.default-radius:50}")
  private String radius;

  public TicketMasterApiClient(WebClient webClient) {
    this.webClient = webClient;
  }

  public TicketMasterApiResponseDto searchNearByEvents(String geoHash, String keyword) {
    String apiUrl =
        UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("apikey", apiKey)
            .queryParam("geoPoint", geoHash)
            .queryParam(
                "keyword", (keyword != null && !keyword.isEmpty()) ? keyword : defaultKeyword)
            .queryParam("radius", radius)
            .toUriString();

    try {
      return webClient
          .get()
          .uri(apiUrl)
          .retrieve()
          .bodyToMono(TicketMasterApiResponseDto.class)
          .doOnError(
              WebClientResponseException.class, e -> log.error("API error: {}", e.getMessage()))
          .onErrorResume(
              e -> {
                log.error("Error fetching events: {}", e.getMessage());
                return Mono.empty();
              })
          .map(this::normalizeIds)
          .block();
    } catch (Exception e) {
      log.error("Unexpected error: {}", e.getMessage());
      return null;
    }
  }

  private TicketMasterApiResponseDto normalizeIds(TicketMasterApiResponseDto response) {
    if (response != null
        && response.getEmbedded() != null
        && response.getEmbedded().getEvents() != null) {
      response
          .getEmbedded()
          .getEvents()
          .forEach(
              event -> {
                if (event != null) {
                  event.setId(IdNormalizerUtil.normalize(event.getId()));
                }
              });
    }
    return response;
  }
}
