package io.github.ziy1.nexevent.entity;

import jakarta.persistence.*;
import java.util.Set;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "event")
public class Event {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "id", nullable = false, unique = true)
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "url")
  private String url;

  @Column(name = "distance")
  private double distance;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "address")
  private String address;

  @ManyToMany
  @JoinTable(
      name = "event_category",
      joinColumns = @JoinColumn(name = "event_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id"))
  private Set<Category> categories;
}
