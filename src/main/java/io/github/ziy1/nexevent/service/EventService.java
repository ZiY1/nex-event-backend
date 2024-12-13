package io.github.ziy1.nexevent.service;

import java.util.List;
import java.util.Set;

import io.github.ziy1.nexevent.dto.EventDto;

public interface EventService {
  List<EventDto> searchNearByEvents(
      String userId, Double latitude, Double longitude, String keyword);

  void setFavoriteEvent(String userId, String eventId);

  Set<EventDto> getFavoriteEvents(String userId);

  void unsetFavoriteEvent(String userId, String eventId);

  List<EventDto> getRecommendedEvents(String userId, Double latitude, Double longitude);
}
