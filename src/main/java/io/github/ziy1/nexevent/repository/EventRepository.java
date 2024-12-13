package io.github.ziy1.nexevent.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ziy1.nexevent.entity.Event;

public interface EventRepository extends JpaRepository<Event, String> {
  @Query("SELECT e.id FROM User u JOIN u.favoriteEvents e WHERE u.id = :userId")
  Set<String> findFavoriteEventIdsByUserId(@Param("userId") String userId);

  @Query("SELECT c.name FROM Event e JOIN e.categories c WHERE e.id = :eventId")
  Set<String> findCategoriesByEventId(@Param("eventId") String eventId);
}
