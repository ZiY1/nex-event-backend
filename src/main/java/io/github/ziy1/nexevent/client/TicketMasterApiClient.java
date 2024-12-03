package io.github.ziy1.nexevent.client;

import io.github.ziy1.nexevent.dto.TicketMasterApiResponseDto;
import io.github.ziy1.nexevent.util.GeoHashUtil;
import io.github.ziy1.nexevent.util.IdNormalizerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TicketMasterApiClient {
  private final WebClient webClient;

  @Value("${ticketmaster.base-url}")
  private String baseUrl;

  @Value("${ticketmaster.api-key}")
  private String apiKey;

  @Value("${ticketmaster.default-keyword}")
  private String defaultKeyword;

  @Value("${ticketmaster.default-radius}")
  private String radius;

  public TicketMasterApiClient(WebClient webClient) {
    this.webClient = webClient;
  }

  public TicketMasterApiResponseDto searchNearByEvents(
      Double latitude, Double longitude, String keyword) {
    String geoHash = GeoHashUtil.encodeGeohash(latitude, longitude, 8);
    String apiUrl =
        UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("apikey", apiKey)
            .queryParam("geoPoint", geoHash)
            .queryParam(
                "keyword", (keyword != null && !keyword.isEmpty()) ? keyword : defaultKeyword)
            .queryParam("radius", radius)
            .toUriString();

    return webClient
        .get()
        .uri(apiUrl)
        .retrieve()
        .bodyToMono(TicketMasterApiResponseDto.class)
        .map(this::normalizeIds)
        .block();
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
