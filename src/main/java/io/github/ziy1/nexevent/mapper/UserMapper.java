package io.github.ziy1.nexevent.mapper;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import io.github.ziy1.nexevent.dto.AuthRegisterRequestDto;
import io.github.ziy1.nexevent.entity.User;

@Component
public class UserMapper {

  public AuthRegisterRequestDto toDto(User user) {
    if (user == null) {
      return null;
    }

    return new AuthRegisterRequestDto(
        user.getUserId(), user.getPassword(), user.getFirstName(), user.getLastName());
  }

  public User toEntity(AuthRegisterRequestDto authRegisterRequestDto) {
    if (authRegisterRequestDto == null) {
      return null;
    }

    return User.builder()
        .userId(authRegisterRequestDto.userId())
        .password(authRegisterRequestDto.password())
        .firstName(authRegisterRequestDto.firstName())
        .lastName(authRegisterRequestDto.lastName())
        .favoriteEvents(new HashSet<>())
        .build();
  }
}
