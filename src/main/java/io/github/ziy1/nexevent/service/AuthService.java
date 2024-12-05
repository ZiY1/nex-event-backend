package io.github.ziy1.nexevent.service;

import io.github.ziy1.nexevent.dto.AuthLoginResponseDto;
import io.github.ziy1.nexevent.dto.AuthRegisterRequestDto;

public interface AuthService {
  AuthRegisterRequestDto register(AuthRegisterRequestDto authRegisterRequestDto);

  AuthLoginResponseDto login(String userId, String password);

  void logout(String token);
}
