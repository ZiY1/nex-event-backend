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
@Table(name = "user")
public class User {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "user_id", nullable = false, unique = true)
  private String userId;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @ManyToMany
  @JoinTable(
      name = "user_event",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "event_id"))
  private Set<Event> favoriteEvents;
}
