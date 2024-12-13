package io.github.ziy1.nexevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ziy1.nexevent.entity.Event;

public interface EventRepository extends JpaRepository<Event, String> {}
