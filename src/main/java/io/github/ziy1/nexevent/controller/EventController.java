package io.github.ziy1.nexevent.controller;

import io.github.ziy1.nexevent.dto.EventDto;
import io.github.ziy1.nexevent.dto.ResponseMessage;
import io.github.ziy1.nexevent.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/nearby")
    public ResponseEntity<ResponseMessage<List<EventDto>>> searchNearByEvents(
            @RequestParam("lat") Double latitude,
            @RequestParam("lon") Double longitude,
            HttpServletRequest request) {

        List<EventDto> events = eventService.searchNearByEvents(getCurrentUserId(), latitude, longitude, null);

        if (events.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ResponseMessage.noContent(request.getRequestURI(), "No nearby events found"));
        }
        return ResponseEntity.ok(ResponseMessage.success(request.getRequestURI(), events));
    }

    @PostMapping("/favorite")
    public ResponseEntity<ResponseMessage<Void>> setFavoriteEvent(
            @RequestParam("event_id") String eventId,
            HttpServletRequest request) {
        eventService.setFavoriteEvent(getCurrentUserId(), eventId);

        return ResponseEntity.ok(ResponseMessage.success(request.getRequestURI()));
    }

    @GetMapping("/favorite")
    public ResponseEntity<ResponseMessage<Set<EventDto>>> getFavoriteEvents(
            HttpServletRequest request) {
        Set<EventDto> favoriteEvents = eventService.getFavoriteEvents(getCurrentUserId());

        return ResponseEntity.ok(ResponseMessage.success(request.getRequestURI(), favoriteEvents));
    }

    @DeleteMapping("favorite")
    public ResponseEntity<ResponseMessage<Void>> unsetFavoriteEvent(
            @RequestParam("event_id") String eventId,
            HttpServletRequest request) {
        eventService.unsetFavoriteEvent(getCurrentUserId(), eventId);

        return ResponseEntity.ok(ResponseMessage.success(request.getRequestURI()));
    }

    @GetMapping("recommend")
    public ResponseEntity<ResponseMessage<List<EventDto>>> getRecommendedEvents(
            @RequestParam("lat") Double latitude,
            @RequestParam("lon") Double longitude,
            HttpServletRequest request) {
        List<EventDto> recommendedEvents = eventService.getRecommendedEvents(getCurrentUserId(), latitude, longitude);

        return ResponseEntity.ok(ResponseMessage.success(request.getRequestURI(), recommendedEvents));
    }
}
