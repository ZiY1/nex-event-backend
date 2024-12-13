package io.github.ziy1.nexevent.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ziy1.nexevent.dto.TicketMasterApiResponseDto;
import io.github.ziy1.nexevent.util.GeoHashUtil;
import io.github.ziy1.nexevent.util.IdNormalizerUtil;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class TicketMasterApiClient {
  private final WebClient webClient;
  private final RedisTemplate<String, Object> redisTemplate;
  private static final String CACHE_PREFIX = "ticketmaster:events:";

  @Value("${ticketmaster.base-url:https://app.ticketmaster.com/discovery/v2/events.json}")
  private String baseUrl;

  @Value("${ticketmaster.api-key}")
  private String apiKey;

  @Value("${ticketmaster.default-keyword:}")
  private String defaultKeyword;

  @Value("${ticketmaster.default-radius:50}")
  private String radius;

  @Value("${geo.hash.precision:8}")
  private int geoHashPrecision;

  @Value("${cache.ttl:3600}")
  private long cacheTtl;

  public TicketMasterApiClient(WebClient webClient, RedisTemplate<String, Object> redisTemplate) {
    this.webClient = webClient;
    this.redisTemplate = redisTemplate;
  }

  public TicketMasterApiResponseDto searchNearByEvents(
      Double latitude, Double longitude, String keyword) {
    String geoHash = GeoHashUtil.encodeGeohash(latitude, longitude, geoHashPrecision);
    String cacheKey = generateCacheKey(geoHash, keyword);

    // Try to get from cache first
    Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
    TicketMasterApiResponseDto cachedResponse = null;

    if (cachedValue instanceof LinkedHashMap) {
      // Configure ObjectMapper to ignore unknown properties
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      cachedResponse = mapper.convertValue(cachedValue, TicketMasterApiResponseDto.class);
    }

    if (cachedResponse != null) {
      log.debug("Cache hit for key: {}", cacheKey);
      return cachedResponse;
    }

    // If not in cache, fetch from API
    log.debug("Cache miss for key: {}, fetching from API", cacheKey);
    TicketMasterApiResponseDto response = fetchFromApi(latitude, longitude, keyword);

    // Store in cache with expiration
    if (response != null) {
      redisTemplate.opsForValue().set(cacheKey, response, cacheTtl, TimeUnit.SECONDS);
    }

    return response;
  }

  private String generateCacheKey(String geoHash, String keyword) {
    return CACHE_PREFIX
        + String.format(
            "geo:%s:kw:%s",
            geoHash, (keyword != null && !keyword.isEmpty()) ? keyword : defaultKeyword);
  }

  private TicketMasterApiResponseDto fetchFromApi(
      Double latitude, Double longitude, String keyword) {
    String geoHash = GeoHashUtil.encodeGeohash(latitude, longitude, geoHashPrecision);
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
