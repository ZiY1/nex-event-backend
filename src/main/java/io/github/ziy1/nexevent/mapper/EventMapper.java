package io.github.ziy1.nexevent.mapper;

import io.github.ziy1.nexevent.dto.EventDto;
import io.github.ziy1.nexevent.dto.TicketMasterApiResponseDto;
import io.github.ziy1.nexevent.entity.Category;
import io.github.ziy1.nexevent.entity.Event;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    public EventDto toDto(Event event, boolean isFavorite) {
        if (event == null) {
            return null;
        }
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getUrl(),
                event.getDistance(),
                event.getImageUrl(),
                event.getAddress(),
                mapCategoriesToNames(event.getCategories()),
                isFavorite
        );

//        return EventDto.builder()
//                .id(event.getId())
//                .name(event.getName())
//                .url(event.getUrl())
//                .distance(event.getDistance())
//                .imageUrl(event.getImageUrl())
//                .address(event.getAddress())
//                .categories(mapCategoriesToNames(event.getCategories()))
//                .favorite(isFavorite)
//                .build();
    }

    public Event toEntity(EventDto eventDto) {
        if (eventDto == null) {
            return null;
        }

        return Event.builder()
                .id(eventDto.id())
                .name(eventDto.name())
                .url(eventDto.url())
                .distance(eventDto.distance())
                .imageUrl(eventDto.imageUrl())
                .address(eventDto.address())
                .categories(mapNamesToCategories(eventDto.categories()))
                .build();
    }

    public EventDto fromTicketMasterEvent(TicketMasterApiResponseDto.Embedded.Event event, boolean isFavorite) {
        if (event == null) {
            return null;
        }

        return new EventDto(
                event.getId(),
                event.getName(),
                event.getUrl(),
                event.getDistance(),
                event.getImageUrl(),
                event.getAddress(),
                event.getCategories(),
                isFavorite
        );
    }

    private Set<String> mapCategoriesToNames(Set<Category> categories) {
        if (categories == null) {
            return Set.of();
        }
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
    }

    private Set<Category> mapNamesToCategories(Set<String> names) {
        if (names == null) {
            return Set.of();
        }
        return names.stream()
                .map(name -> Category.builder()
                        .name(name)
                        .build())
                .collect(Collectors.toSet());
    }
}
