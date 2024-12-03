package io.github.ziy1.nexevent.dto;

import java.util.Set;

public record EventDto(
    String id,
    String name,
    String url,
    double distance,
    String imageUrl,
    String address,
    Set<String> categories,
    boolean favorite) {}
