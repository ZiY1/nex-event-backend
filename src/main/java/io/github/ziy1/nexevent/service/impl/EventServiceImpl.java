package io.github.ziy1.nexevent.service.impl;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

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

  @Value("${executor.thread.pool.size:2}")
  private int threadPoolSize;

  @Value("${executor.core.pool.size:1}")
  private int corePoolSize;

  @Value("${executor.queue.size:10}")
  private int queueSize;

  @Value("${executor.keep.alive.time:60}")
  private long keepAliveTime;

  @Value("${executor.shutdown.timeout:60}")
  private long shutdownTimeout;

  private final ExecutorService executorService;

  public EventServiceImpl(
      TicketMasterApiClient ticketMasterApiClient,
      UserRepository userRepository,
      EventRepository eventRepository,
      CategoryRepository categoryRepository,
      EventMapper eventMapper,
      RedisTemplate<String, Object> redisTemplate,
      ExecutorService executorService) {
    this.ticketMasterApiClient = ticketMasterApiClient;
    this.userRepository = userRepository;
    this.eventRepository = eventRepository;
    this.categoryRepository = categoryRepository;
    this.eventMapper = eventMapper;
    this.redisTemplate = redisTemplate;
    this.executorService = executorService;
  }

  @Override
  public List<EventDto> searchNearByEvents(
      String userId, Double latitude, Double longitude, String keyword) {
    String geoHash = GeoHashUtil.encodeGeohash(latitude, longitude, geoHashPrecision);
    String cacheKey = generateCacheKey(geoHash, keyword);

    // Fetch from cache
    TicketMasterApiResponseDto cachedResponse = getEventsFromCache(cacheKey);
    if (cachedResponse != null) {
      return toEventDtosWithUserFavorites(cachedResponse, userId);
    }

    // Call Ticketmaster API
    TicketMasterApiResponseDto apiResponse =
        ticketMasterApiClient.searchNearByEvents(geoHash, keyword);

    // Store in cache and database
    if (apiResponse != null) {
      redisTemplate.opsForValue().set(cacheKey, apiResponse, cacheTtl, TimeUnit.SECONDS);
      saveNearByEvents(toEventDtosWithUserFavorites(apiResponse, userId));
    }

    return toEventDtosWithUserFavorites(apiResponse, userId);
  }

  private String generateCacheKey(String geoHash, String keyword) {
    return CACHE_PREFIX
        + String.format(
            "geo:%s:kw:%s",
            geoHash, (keyword != null && !keyword.isEmpty()) ? keyword : defaultKeyword);
  }

  private TicketMasterApiResponseDto getEventsFromCache(String cacheKey) {
    Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
    if (cachedValue instanceof LinkedHashMap) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return mapper.convertValue(cachedValue, TicketMasterApiResponseDto.class);
    }
    return null;
  }

  private List<EventDto> toEventDtosWithUserFavorites(
      TicketMasterApiResponseDto apiResponse, String userId) {
    Set<String> userFavoriteEventIds = eventRepository.findFavoriteEventIdsByUserId(userId);

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
    Map<String, Category> categoryCache = new HashMap<>();

    // Create or reuse categories
    for (EventDto eventDto : eventDtos) {
      eventDto
          .categories()
          .forEach(
              categoryName -> {
                categoryCache.computeIfAbsent(
                    categoryName,
                    name ->
                        categoryRepository
                            .findByNameIgnoreCase(name)
                            .orElseGet(
                                () -> categoryRepository.save(new Category(null, name, null))));
              });
    }

    // Batch save events
    List<Event> events =
        eventDtos.stream()
            .map(
                dto -> {
                  Event event = eventMapper.toEntity(dto);
                  Set<Category> categories =
                      dto.categories().stream().map(categoryCache::get).collect(Collectors.toSet());
                  event.setCategories(categories);
                  return event;
                })
            .toList();

    eventRepository.saveAll(events);
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
    // Step 1: Get favorite event IDs
    Set<String> favoriteEventIds = eventRepository.findFavoriteEventIdsByUserId(userId);

    // Step 2: Get and sort categories by favorite event occurrence
    List<String> sortedCategories = getSortedCategoriesByFavorites(favoriteEventIds);

    // Step 3: Search based on sorted category, filter out favorite events, sort by distance
    return fetchAndProcessEvents(userId, latitude, longitude, sortedCategories);
  }

  private List<String> getSortedCategoriesByFavorites(Set<String> favoriteEventIds) {
    Map<String, Integer> categoryCounts = new HashMap<>();
    for (String eventId : favoriteEventIds) {
      Set<String> categories = eventRepository.findCategoriesByEventId(eventId);
      for (String category : categories) {
        categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
      }
    }
    return categoryCounts.entrySet().stream()
        .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Descending order
        .map(Map.Entry::getKey)
        .toList();
  }

  private List<EventDto> fetchAndProcessEvents(
      String userId, Double latitude, Double longitude, List<String> sortedCategories) {
    Set<String> seenEventIds = new HashSet<>();
    List<Future<List<EventDto>>> futures = new ArrayList<>();

    // Submit API calls to thread pool
    for (String categoryName : sortedCategories) {
      futures.add(
          executorService.submit(
              () -> searchNearByEvents(userId, latitude, longitude, categoryName)));
    }

    List<EventDto> recommendedEvents = new ArrayList<>();
    for (Future<List<EventDto>> future : futures) {
      try {
        List<EventDto> eventDtos = future.get(); // Blocking until the result is available
        eventDtos.stream()
            .filter(eventDto -> !eventDto.favorite())
            .filter(eventDto -> seenEventIds.add(eventDto.id()))
            .sorted(Comparator.comparing(EventDto::distance))
            .forEach(recommendedEvents::add);
      } catch (Exception e) {
        log.error("Error fetching events: {}", e.getMessage());
      }
    }

    return recommendedEvents;
  }

  @PreDestroy
  public void shutdownExecutorService() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
    log.info("ExecutorService shut down gracefully.");
  }
}
