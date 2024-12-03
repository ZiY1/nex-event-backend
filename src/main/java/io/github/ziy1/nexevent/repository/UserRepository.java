package io.github.ziy1.nexevent.repository;

import io.github.ziy1.nexevent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {}
