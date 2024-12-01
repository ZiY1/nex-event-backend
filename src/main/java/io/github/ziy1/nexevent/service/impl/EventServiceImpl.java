package io.github.ziy1.nexevent.service.impl;

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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final TicketMasterApiClient ticketMasterApiClient;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(TicketMasterApiClient ticketMasterApiClient,
                            UserRepository userRepository,
                            EventRepository eventRepository,
                            CategoryRepository categoryRepository,
                            EventMapper eventMapper) {
        this.ticketMasterApiClient = ticketMasterApiClient;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    public List<EventDto> searchNearByEvents(String userId, Double latitude, Double longitude, String keyword) {
        Set<String> userFavoriteEventIds = userRepository.findById(userId)
                .map(user -> user.getFavoriteEvents().stream()
                        .map(Event::getId)
                        .collect(Collectors.toSet()))
                .orElse(new HashSet<>());

        final TicketMasterApiResponseDto apiResponse = ticketMasterApiClient.searchNearByEvents(latitude, longitude, keyword);

        if (apiResponse != null && apiResponse.getEmbedded() != null) {
            List<EventDto> eventDtos = apiResponse.getEmbedded().getEvents().stream()
                    .map(event -> {
                        return eventMapper.fromTicketMasterEvent(
                                event,
                                userFavoriteEventIds.contains(event.getId())
                        );
                    })
                    .toList();

            saveNearByEvents(eventDtos);

            return eventDtos;
        }

        return List.of();
    }

    private void saveNearByEvents(List<EventDto> eventDtos) {
        for (EventDto eventDto : eventDtos) {
            Set<Category> categories = eventDto.categories().stream()
                    .map(name -> categoryRepository.findByNameIgnoreCase(name)
                            .orElseGet(() -> categoryRepository.save(new Category(null, name, null))))
                    .collect(Collectors.toSet());

            Event event = eventMapper.toEntity(eventDto);

            event.setCategories(categories);

            eventRepository.save(event);
        }
    }

    @Override
    public void setFavoriteEvent(String userId, String eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Event favoriteEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        user.getFavoriteEvents().add(favoriteEvent);
        userRepository.save(user);
    }

    @Override
    public Set<EventDto> getFavoriteEvents(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getFavoriteEvents().stream()
                .map(event -> {
                    return eventMapper.toDto(event, true);
                })
                .collect(Collectors.toSet());
    }

    @Override
    public void unsetFavoriteEvent(String userId, String eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Event favoriteEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        user.getFavoriteEvents().remove(favoriteEvent);
        userRepository.save(user);
    }

    @Override
    public List<EventDto> getRecommendedEvents(String userId, Double latitude, Double longitude) {
        // Step 1: Retrieve categories from favorite events and count occurrences
        Map<String, Integer> categoryCounts = new HashMap<>();

        Set<EventDto> favoriteEventDtos = getFavoriteEvents(userId);
        for (EventDto eventDto : favoriteEventDtos) {
            eventDto.categories().forEach(category -> {
                categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
            });
        }

        // Step 2: Sort categories by occurrence count (most to least)
        List<String> sortedCategories = categoryCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        // Step 3: Search based on sorted category, filter out favorite events, sort by distance
        List<EventDto> recommendedEvents = new ArrayList<>();
        for (String categoryName : sortedCategories) {
            List<EventDto> eventDtos = searchNearByEvents(userId, latitude, longitude, categoryName);

            eventDtos.stream()
                    .filter(eventDto -> !eventDto.favorite())
                    .sorted(Comparator.comparing(EventDto::distance))
                    .forEach(recommendedEvents::add);
        }

        return recommendedEvents;
    }
}
