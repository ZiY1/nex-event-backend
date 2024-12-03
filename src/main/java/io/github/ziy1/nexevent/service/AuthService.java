package io.github.ziy1.nexevent.service;

import io.github.ziy1.nexevent.dto.AuthRegisterRequestDto;

public interface AuthService {
  AuthRegisterRequestDto register(AuthRegisterRequestDto authRegisterRequestDto);

  String login(String userId, String password);

  void logout(String token);
}
