package io.github.ziy1.nexevent.repository;

import io.github.ziy1.nexevent.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, String> {}
