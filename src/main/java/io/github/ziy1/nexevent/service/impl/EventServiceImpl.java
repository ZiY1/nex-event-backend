package io.github.ziy1.nexevent.service.impl;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import io.github.ziy1.nexevent.client.TicketMasterApiClient;
import io.github.ziy1.nexevent.dto.EventDto;
import io.github.ziy1.nexevent.dto.TicketMasterApiResponseDto;
import io.github.ziy1.nexevent.entity.Category;
import io.github.ziy1.nexevent.entity.Event;
import io.github.ziy1.nexevent.entity.User;
import io.github.ziy1.nexevent.mapper.EventMapper;
import io.github.ziy1.nexevent.repository.CategoryRepository;
import io.github.ziy1.nexevent.repository.EventRepository;
import io.github.ziy1.nexevent.repository.UserRepository;
import io.github.ziy1.nexevent.service.EventService;
import io.github.ziy1.nexevent.util.GeoHashUtil;
import io.github.ziy1.nexevent.util.StreamUtils;

@Slf4j
@Service
public class EventServiceImpl implements EventService {
  private final TicketMasterApiClient ticketMasterApiClient;
  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final CategoryRepository categoryRepository;
  private final EventMapper eventMapper;
  private final RedisTemplate<String, Object> redisTemplate;
  private static final String CACHE_PREFIX = "ticketmaster:events:";

  @Value("${ticketmaster.default-keyword:}")
  private String defaultKeyword;

  @Value("${geo.hash.precision:6}")
  private int geoHashPrecision;

  @Value("${cache.ttl:3600}")
  private long cacheTtl;

  public EventServiceImpl(
      TicketMasterApiClient ticketMasterApiClient,
      UserRepository userRepository,
      EventRepository eventRepository,
      CategoryRepository categoryRepository,
      EventMapper eventMapper,
      RedisTemplate<String, Object> redisTemplate) {
    this.ticketMasterApiClient = ticketMasterApiClient;
    this.userRepository = userRepository;
    this.eventRepository = eventRepository;
    this.categoryRepository = categoryRepository;
    this.eventMapper = eventMapper;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public List<EventDto> searchNearByEvents(
      String userId, Double latitude, Double longitude, String keyword) {
    final String geoHash = GeoHashUtil.encodeGeohash(latitude, longitude, geoHashPrecision);
    final String cacheKey = generateCacheKey(geoHash, keyword);

    // Try to get from cache first
    final Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
    TicketMasterApiResponseDto cachedResponse = null;

    if (cachedValue instanceof LinkedHashMap) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      cachedResponse = mapper.convertValue(cachedValue, TicketMasterApiResponseDto.class);
    }

    // If in cache, return the result
    if (cachedResponse != null) {
      log.debug("Cache hit for key: {}", cacheKey);
      return toEventDtosWithUserFavorites(cachedResponse, userId);
    }

    // If not in cache, fetch from API, store in cache, and save to database
    log.debug("Cache miss for key: {}, fetching from API", cacheKey);
    final TicketMasterApiResponseDto apiResponse =
        ticketMasterApiClient.searchNearByEvents(geoHash, keyword);

    // Store in cache with expiration
    if (apiResponse != null) {
      redisTemplate.opsForValue().set(cacheKey, apiResponse, cacheTtl, TimeUnit.SECONDS);
    }

    List<EventDto> eventDtos = toEventDtosWithUserFavorites(apiResponse, userId);

    if (eventDtos != null) {
      saveNearByEvents(eventDtos);
    }

    return eventDtos;
  }

  private String generateCacheKey(String geoHash, String keyword) {
    return CACHE_PREFIX
        + String.format(
            "geo:%s:kw:%s",
            geoHash, (keyword != null && !keyword.isEmpty()) ? keyword : defaultKeyword);
  }

  private List<EventDto> toEventDtosWithUserFavorites(
      TicketMasterApiResponseDto apiResponse, String userId) {
    Set<String> userFavoriteEventIds =
        userRepository
            .findById(userId)
            .map(
                user ->
                    user.getFavoriteEvents().stream().map(Event::getId).collect(Collectors.toSet()))
            .orElse(new HashSet<>());

    if (apiResponse != null && apiResponse.getEmbedded() != null) {
      return apiResponse.getEmbedded().getEvents().stream()
          .filter(StreamUtils.distinctByKey(TicketMasterApiResponseDto.Embedded.Event::getId))
          .map(
              event ->
                  eventMapper.fromTicketMasterEvent(
                      event, userFavoriteEventIds.contains(event.getId())))
          .toList();
    }

    return List.of();
  }

  private void saveNearByEvents(List<EventDto> eventDtos) {
    for (EventDto eventDto : eventDtos) {
      Set<Category> categories =
          eventDto.categories().stream()
              .map(
                  name ->
                      categoryRepository
                          .findByNameIgnoreCase(name)
                          .orElseGet(() -> categoryRepository.save(new Category(null, name, null))))
              .collect(Collectors.toSet());

      Event event = eventMapper.toEntity(eventDto);

      event.setCategories(categories);

      eventRepository.save(event);
    }
  }

  @Override
  public void setFavoriteEvent(String userId, String eventId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    Event favoriteEvent =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    user.getFavoriteEvents().add(favoriteEvent);
    userRepository.save(user);
  }

  @Override
  public Set<EventDto> getFavoriteEvents(String userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    return user.getFavoriteEvents().stream()
        .map(
            event -> {
              return eventMapper.toDto(event, true);
            })
        .collect(Collectors.toSet());
  }

  @Override
  public void unsetFavoriteEvent(String userId, String eventId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    Event favoriteEvent =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    user.getFavoriteEvents().remove(favoriteEvent);
    userRepository.save(user);
  }

  @Override
  public List<EventDto> getRecommendedEvents(String userId, Double latitude, Double longitude) {
    // TODO: get only favorite event ids from db
    // Step 1: Retrieve categories from favorite events and count occurrences
    Map<String, Integer> categoryCounts = new HashMap<>();

    Set<EventDto> favoriteEventDtos = getFavoriteEvents(userId);
    for (EventDto eventDto : favoriteEventDtos) {
      eventDto
          .categories()
          .forEach(
              category -> {
                categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
              });
    }

    // Step 2: Sort categories by occurrence count (most to least)
    List<String> sortedCategories =
        categoryCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(Map.Entry::getKey)
            .toList();

    // Step 3: Search based on sorted category, filter out favorite events, sort by distance
    Set<String> seenEventIds = new HashSet<>();
    List<EventDto> recommendedEvents = new ArrayList<>();

    for (String categoryName : sortedCategories) {
      List<EventDto> eventDtos = searchNearByEvents(userId, latitude, longitude, categoryName);

      eventDtos.stream()
          .filter(eventDto -> !eventDto.favorite())
          .filter(eventDto -> seenEventIds.add(eventDto.id()))
          .sorted(Comparator.comparing(EventDto::distance))
          .forEach(recommendedEvents::add);
    }

    return recommendedEvents;
  }
}
